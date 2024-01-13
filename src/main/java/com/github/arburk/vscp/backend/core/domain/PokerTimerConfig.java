package com.github.arburk.vscp.backend.core.domain;

import java.util.List;

public record PokerTimerConfig(Short roundInMinutes, Short warningTimeInMinutes, List<Short> blindLevels) {

}
