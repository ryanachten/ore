package com.ryanachten.ore.gateway.config;

import com.ryanachten.ore.common.contracts.VehicleDto;
import com.ryanachten.ore.gateway.models.Vehicle;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ModelMapper {
  Vehicle toVehicle(VehicleDto dto);
}
