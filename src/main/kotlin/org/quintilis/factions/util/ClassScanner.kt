package org.quintilis.factions.util

import org.bukkit.plugin.java.JavaPlugin
import java.util.jar.JarFile

object ClassScanner {
    inline fun <reified T, reified A : Annotation> findClasses(
        plugin: JavaPlugin,
        packageName: String,
    ): List<Class<T>> {
        val classes = mutableListOf<Class<T>>()

        val targetType = T::class.java
        val annotationType = A::class.java

        val srcPath = plugin.javaClass.protectionDomain.codeSource.location
        // Decodificação simples para evitar erros com espaços no caminho
        val jarPath = srcPath.path.replace("%20", " ")

        try {
            // Verificação de segurança: Se estiver rodando em IDE (pasta) em vez de JAR, isso evita erro
            if (!jarPath.endsWith(".jar")) return classes

            JarFile(jarPath).use { jarFile ->
                val entries = jarFile.entries()

                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name

                    // 1. Filtra apenas arquivos .class
                    if (name.endsWith(".class")) {
                        // 2. Transforma caminho em nome de pacote: org/teste/Classe.class -> org.teste.Classe
                        val className = name.replace('/', '.').removeSuffix(".class")

                        // 3. Verifica se está dentro do pacote solicitado
                        if (className.startsWith(packageName)) {
                            try {
                                // false = Não inicializa blocos estáticos da classe (performance e segurança)
                                val clazz = Class.forName(className, false, plugin.javaClass.classLoader)

                                // 4. Verifica Anotação E Tipo (Herança/Interface)
                                if (clazz.isAnnotationPresent(annotationType) && targetType.isAssignableFrom(clazz)) {
                                    @Suppress("UNCHECKED_CAST")
                                    classes.add(clazz as Class<T>)
                                }
                            } catch (e: Throwable) {
                                // Ignora classes que não podem ser carregadas (ex: dependências faltando)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return classes
    }
}