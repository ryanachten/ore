package com.ryanachten.ore.vehicle.config;

import com.ryanachten.ore.common.contracts.VehicleDto;
import com.ryanachten.ore.common.contracts.WorldDto;
import com.ryanachten.ore.vehicle.models.Vehicle;
import com.ryanachten.ore.vehicle.models.World;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ModelMapper {
  VehicleDto toVehicleDto(Vehicle vehicle);

  World toWorld(WorldDto dto);
}
