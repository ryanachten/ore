package com.ryanachten.ore.gateway.models;

import java.util.Collection;

public record CurrentWorldState(long tick, Collection<Vehicle> vehicles) {}
