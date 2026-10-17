package com.ryanachten.ore.gateway.models;

import com.ryanachten.ore.common.contracts.Vehicle;
import java.util.Collection;

public record CurrentWorldState(long tick, Collection<Vehicle> vehicles) {}
