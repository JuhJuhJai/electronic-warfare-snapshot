package ew.server;

import ew.playerData.Commander;

public record GameResult(Commander commanderInGame, GameResultType commanderResult) {}
