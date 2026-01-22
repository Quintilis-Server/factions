package org.quintilis.factions.entities

import org.jdbi.v3.sqlobject.transaction.Transaction
import org.quintilis.factions.annotations.Column
import org.quintilis.factions.annotations.PrimaryKey
import org.quintilis.factions.annotations.TableName
import org.quintilis.factions.annotations.Transient as CustomTransient
import org.quintilis.factions.managers.DatabaseManager
import kotlin.jvm.Transient
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Propriedade da classe para achar a anotação `@Column`.
 * Lógica:
 * 1. Pega a anotação.
 * 2. Se o nome na anotação não for nulo e nem vazio, usa ele.
 * 3. Caso contrário, usa o nome da variável.
 */
private val KProperty1<*, *>.columnName: String
    get() {
        val annotation = this.findAnnotation<Column>()
        // Se a anotação existir E o nome não estiver em branco, usa o nome da anotação.
        // O takeIf retorna null se a condição for falsa (se for string vazia).
        return annotation?.name?.takeIf { it.isNotBlank() } ?: this.name
    }
/**
 * Classe abstrata que outras entidade herdam
 * Essa classe precisa da `@TableName`, `@Column` e pelo menos uma `@PrimaryKey`
 */
abstract class BaseEntity {
    @delegate:Transient
    val tableName: String by lazy {
        this::class.findAnnotation<TableName>()?.name
            ?: throw IllegalArgumentException("A classe ${this::class.simpleName} não tem @TableName")
    }

    @delegate:Transient
    val primaryKeyProperties: List<KProperty1<BaseEntity, *>> by lazy {
        val props = this::class.memberProperties
            .filter { it.hasAnnotation<PrimaryKey>() }
            .map {
                @Suppress("UNCHECKED_CAST")
                it as KProperty1<BaseEntity, *>
            }

        props.ifEmpty {
            // Fallback para "id" se não tiver anotação
            val idProp = this::class.memberProperties.find { it.name == "id" }
            if (idProp != null) listOf(idProp as KProperty1<BaseEntity, *>) else emptyList()
        }
    }

    @delegate:Transient
    val primaryKeyColumnNames: List<String> by lazy {
        primaryKeyProperties.map { it.columnName }
    }

    @Transient
    val primaryKeyPropertyNames: List<String> = primaryKeyProperties.map { it.name }

    /**
     * Salva a entidade na database
     * Ela da update se ele ja existir dentro da database, conforme o `@PrimaryKey`
     * ou ela insere se não existir ja
     * @param T É uma BaseEntity, preferencialmente precisa ser a propria classe, o cast é automático para essa função
     * @return o tipo genérico passado antes
     */
    @Transaction
    fun <T : BaseEntity> save(): T {
        // 1. Verificação de Auto-Increment (Serial)
        // Só consideramos auto-increment se tiver APENAS UMA PK e o valor dela for NULL.
        val singlePk = primaryKeyProperties.singleOrNull()
        val isAutoIncrementInsert = singlePk != null && singlePk.get(this) == null

        // 2. Filtra quais propriedades vamos enviar para o banco
        val propertiesToSave = this::class.primaryConstructor?.parameters
            ?.mapNotNull { param ->
                this::class.memberProperties.find { prop -> prop.name == param.name && !prop.hasAnnotation<CustomTransient>() }
            }
            ?.filter { prop ->
                // Se for um insert de ID automático, removemos a PK da lista de colunas para o Postgres gerar
                if (isAutoIncrementInsert && prop.name == singlePk!!.name) {
                    false
                } else {
                    true
                }
            } ?: emptyList()

        // 3. Montagem do SQL
        val columns = propertiesToSave.joinToString(", ") { it.columnName }
        val namedParams = propertiesToSave.joinToString(", ") { ":${it.name}" }

        val sql: String

        if (isAutoIncrementInsert) {
            // CASO A: Insert Simples (Deixa o banco gerar o ID)
            // Não usamos ON CONFLICT aqui porque não podemos conflitar com NULL
            sql = """
                INSERT INTO $tableName ($columns) VALUES ($namedParams)
                RETURNING *
            """.trimIndent()
        } else {
            // CASO B: Upsert (Chave Composta ou Update de ID existente)
            // Precisamos listar TODAS as chaves no ON CONFLICT (ex: clan_id, uuid)
            val conflictTarget = primaryKeyColumnNames.joinToString(", ")

            // Setamos todos os campos EXCETO as chaves primárias
            val updateSet = propertiesToSave
                .filter { prop ->
                    // Não atualizamos colunas que fazem parte da PK
                    primaryKeyProperties.none { pk -> pk.name == prop.name }
                }
                .joinToString(", ") { "${it.columnName} = :${it.name}" }

            // Se o updateSet estiver vazio (ex: tabela só tem PKs), fazemos DO NOTHING
            val doAction = if (updateSet.isNotEmpty()) "DO UPDATE SET $updateSet" else "DO NOTHING"

            sql = """
                INSERT INTO $tableName ($columns) VALUES ($namedParams)
                ON CONFLICT ($conflictTarget) $doAction
                RETURNING *
            """.trimIndent()
        }

        // 4. Execução JDBI
        return DatabaseManager.jdbi.inTransaction<T, Exception> { handle ->
            val update = handle.createUpdate(sql)

            propertiesToSave.forEach { prop ->
                @Suppress("UNCHECKED_CAST")
                val typedProp = prop as KProperty1<BaseEntity, *>
                var value = typedProp.get(this)

                // Tratamento para Enums (Salvar como String)
                if (value is Enum<*>) {
                    value = value.name
                }

                update.bind(prop.name, value)
            }

            update.executeAndReturnGeneratedKeys()
                .mapTo(this::class.java as Class<T>)
                .one()
        }
    }
}