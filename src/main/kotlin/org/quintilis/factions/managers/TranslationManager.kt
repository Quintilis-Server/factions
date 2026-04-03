package org.quintilis.factions.managers

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.net.URI
import java.util.Collections
import java.util.Enumeration
import java.util.Locale
import java.util.ResourceBundle
import java.util.jar.JarFile

object TranslationManager {
    /**
     * Inicia o sistema de traduções dinâmico.
     * @param plugin A instância principal do seu plugin (Factions)
     */
    fun registerTranslations(plugin: JavaPlugin) {
        val translationKey = Key.key("factions", "translations")

        // Usa a sua classe moderna (livre de Deprecation e perfeita pro MiniMessage)
        val store = MiniMessageTranslationStore.create(translationKey)

        // 1. Cria a pasta translations se ela não existir e exporta os arquivos base
        val langFolder = File(plugin.dataFolder, "translations")
        if (!langFolder.exists()) {
            langFolder.mkdirs()
        }

        extractDefaultTranslations(plugin)

        // 2. Lista todos os arquivos .yml ou .yaml dentro da pasta
        val files = langFolder.listFiles()?.filter { it.extension == "yml" || it.extension == "yaml" }

        if (files.isNullOrEmpty()) {
            plugin.logger.warning("Nenhum arquivo de tradução encontrado na pasta translations!")
            return
        }

        var loadedCount = 0

        // 3. Lê cada arquivo dinamicamente
        for (file in files) {
            // Pega o nome do arquivo (ex: "pt_BR" vira o idioma Português do Brasil)
            // Pega o nome do arquivo (ex: "pt_BR") e troca para tag ("pt-BR")
            val localeTag = file.nameWithoutExtension.replace("_", "-")

            // Cria o Locale usando o padrão moderno do Java sem Deprecation
            val locale = Locale.forLanguageTag(localeTag)

            // Carrega o arquivo YAML usando a API do Bukkit
            val yaml = YamlConfiguration.loadConfiguration(file)

            // Transforma o YAML no Bundle falso que criamos abaixo
            val bundle = YamlResourceBundle(yaml)

            // Registra todas as mensagens no idioma daquele arquivo
            store.registerAll(locale, bundle, false)

            loadedCount++
            plugin.logger.info("Traduções carregadas do arquivo: ${file.name} ($locale)")
        }

        // 4. Injeta as traduções no Tradutor Global do Servidor
        GlobalTranslator.translator().addSource(store)

        plugin.logger.info("$loadedCount pacotes de idioma registrados com sucesso.")
    }

    /**
     * Abre o arquivo .jar do plugin, procura a pasta 'translations'
     * e extrai TODOS os arquivos de idioma para a pasta do servidor.
     */
    private fun extractDefaultTranslations(plugin: JavaPlugin) {
        try {
            // Pega o caminho do seu plugin.jar rodando no servidor
            val jarUri: URI = plugin.javaClass.protectionDomain.codeSource.location.toURI()
            val jarFile = File(jarUri)

            if (!jarFile.isFile) return // Prevenção caso esteja rodando direto pela IDE em modo debug solto

            JarFile(jarFile).use { jar ->
                val entries = jar.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name

                    // Se for um arquivo dentro da pasta translations/ e for um .yml
                    if (name.startsWith("translations/") && (name.endsWith(".yml") || name.endsWith(".yaml"))) {
                        val outFile = File(plugin.dataFolder, name)

                        // Salva apenas se o dono do servidor não tiver gerado ainda
                        if (!outFile.exists()) {
                            plugin.saveResource(name, false)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("Falha ao extrair idiomas padrão do .jar: ${e.message}")
        }
    }


    class YamlResourceBundle(private val yaml: YamlConfiguration) : ResourceBundle() {

        // Entrega o texto do YAML quando o Kyori pedir a tradução
        override fun handleGetObject(key: String): Any? {
            return yaml.getString(key)
        }

        // Passa a lista de todas as chaves do YAML para o Kyori registrar
        override fun getKeys(): Enumeration<String> {
            val keys = yaml.getKeys(true).filter { yaml.isString(it) }
            return Collections.enumeration(keys)
        }
    }
}