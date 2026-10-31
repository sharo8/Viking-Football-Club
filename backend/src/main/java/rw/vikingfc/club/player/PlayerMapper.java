package rw.vikingfc.club.player;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlayerMapper {

    @Mapping(target = "position", source = "position.label")
    @Mapping(target = "line", source = "lineName")
    @Mapping(target = "captainRank", ignore = true)
    PlayerDtos.Public toPublic(Player player);

    @Mapping(target = "type", source = "photoType")
    PlayerDtos.Photo toPhoto(PlayerPhoto photo);
}
