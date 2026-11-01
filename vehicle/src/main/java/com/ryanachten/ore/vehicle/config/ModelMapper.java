package com.ryanachten.ore.vehicle.config;

import com.ryanachten.ore.common.contracts.VehicleDto;
import com.ryanachten.ore.vehicle.models.Vehicle;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ModelMapper {
  VehicleDto toVehicleDto(Vehicle vehicle);
}
