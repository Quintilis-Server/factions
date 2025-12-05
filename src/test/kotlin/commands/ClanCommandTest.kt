package commands

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.ServerMock
import be.seeseemelk.mockbukkit.entity.PlayerMock
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import org.bukkit.plugin.PluginDescriptionFile
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.quintilis.factions.Factions
import org.quintilis.factions.commands.clan.ClanCommand
import java.io.File

class ClanCommandTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: Factions
    private lateinit var player: PlayerMock

    @BeforeEach
    fun setup() {
        server = MockBukkit.mock()

        // 1. Defina o arquivo manualmente
        val file = File("src/main/resources/plugin.yml")

        // 2. Crie a descrição do plugin manualmente
        val description = PluginDescriptionFile(file.inputStream())

        // 3. Carregue o plugin usando a descrição
        plugin = MockBukkit.load(Factions::class.java, description)

        player = server.addPlayer("AbacateGamer285")
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `test clan create success`(){
        player.performCommand("clan create Alpha ALP")

        val message = player.nextComponentMessage()

        assertNotNull(message, "O jogador deveria ter recebido uma mensagem")

        assertTrue(message is TranslatableComponent, "A mensagem deveria ser traduzível")
        val translatable = message as TranslatableComponent

        assertEquals("clan.create.response", translatable.key())

        val args = translatable.args()

        val clanNameArg = args.firstOrNull {
            // A lógica exata depende de como o MiniMessage/Argument empacota,
            // mas geralmente o valor está no 'content()' do componente filho
            (it as? TextComponent)?.content() == "Alpha"
        }

        assertNotNull(clanNameArg, "Deveria ter o argumento 'Alpha' na mensagem")
    }
}