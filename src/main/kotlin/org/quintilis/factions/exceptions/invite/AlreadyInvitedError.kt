package org.quintilis.factions.exceptions.invite

import net.kyori.adventure.text.minimessage.translation.Argument
import org.quintilis.factions.exceptions.BaseError

class AlreadyInvitedError(val name: String): BaseError("Already invited", "error.already_invited", Argument.string("name", name))