package org.quintilis.factions.results

/**
 * Resultado de operações de clã.
 * Usa sealed class para garantir tipo seguro e exaustividade no when.
 */
sealed class Result {
    /**
     * Operação bem-sucedida.
     * @param messageKey Chave de tradução para mensagem de sucesso (opcional)
     * @param args Argumentos da mensagem (opcional)
     */
    data class Success(
        val messageKey: String? = null,
        val args: Map<String, Any> = emptyMap()
    ) : Result()
    
    /**
     * Operação falhou.
     * @param messageKey Chave de tradução para mensagem de erro
     * @param args Argumentos da mensagem
     */
    data class Error(
        val messageKey: String,
        val args: Map<String, Any> = emptyMap()
    ) : Result()
}
