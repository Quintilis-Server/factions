package org.quintilis.factions.util

import org.bukkit.plugin.java.JavaPlugin
import java.util.jar.JarFile

object ClassScanner {
    inline fun <reified T, reified A: Annotation> findClasses(
        plugin: JavaPlugin,
        packageName: String,
    ): List<Class<T>>{
        val classes = mutableListOf<Class<T>>()

        val targetType = T::class.java
        val annotationType = A::class.java

        val srcPath = plugin.javaClass.protectionDomain.codeSource.location
        val jarPath = srcPath.path.replace("%20", " ")

        try{
            JarFile(jarPath).use { jarFile ->
                val entries = jarFile.entries()

                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name

                    if(name.endsWith(".class") && name.replace('/', '.').startsWith(packageName)) {
                        val className = name.replace('/', '.')

                        try{
                            val clazz = Class.forName(className, false, plugin.javaClass.classLoader)

                            if(clazz.isAnnotationPresent(annotationType)){}

                        }
                    }
                }
            }
        }
    }
}