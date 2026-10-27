package ew.engine.board.state;

import ew.playerData.Commander;

public record GameResult(Commander commanderInGame, GameResultType commanderResult) {}
