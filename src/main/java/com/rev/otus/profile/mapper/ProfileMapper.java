package com.rev.otus.profile.mapper;

import com.rev.otus.profile.CustomerProfile;
import com.rev.otus.profile.dto.ProfileDto;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Маппер Entity ↔ DTO для профиля пользователя.
 *
 * <p><b>Назначение:</b> преобразует {@link CustomerProfile} в {@link ProfileDto}
 * и обратно, а также обновляет Entity из DTO.
 *
 * <p><b>Особенности:</b>
 * <ul>
 *   <li>{@code id}, {@code keycloakUserId}, {@code createdAt}, {@code updatedAt}
 *       устанавливаются сервером — не маппятся из DTO</li>
 *   <li>{@code password} в Entity нет — при маппинге в DTO он игнорируется</li>
 *   <li>PUT — полная замена ({@code null} обнуляет поля)</li>
 *   <li>PATCH — частичное обновление ({@code null} игнорируется)</li>
 * </ul>
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ProfileMapper {

    /**
     * Entity → DTO.
     *
     * <p>Поле {@code password} игнорируется — Entity его не хранит.
     *
     * @param entity сущность профиля
     * @return DTO для ответа API
     */
    @Mapping(target = "password", ignore = true)
    ProfileDto toDto(CustomerProfile entity);

    /**
     * Список Entity → список DTO.
     *
     * @param entities список сущностей
     * @return список DTO
     */
    List<ProfileDto> toDtoList(List<CustomerProfile> entities);

    /**
     * DTO → Entity (создание).
     *
     * <p>{@code id}, {@code keycloakUserId}, {@code createdAt}, {@code updatedAt}
     * устанавливаются сервером.
     *
     * @param dto входящий DTO
     * @return новая сущность
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakUserId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CustomerProfile toEntity(ProfileDto dto);

    /**
     * Полное обновление Entity из DTO (PUT).
     *
     * <p>{@code null} поля из DTO <b>обнуляют</b> поля Entity
     * (благодаря {@link NullValuePropertyMappingStrategy#SET_TO_NULL}).
     *
     * @param dto    входящий DTO
     * @param entity сущность для обновления
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakUserId", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    void updateEntity(ProfileDto dto, @MappingTarget CustomerProfile entity);

    /**
     * Частичное обновление Entity из DTO (PATCH).
     *
     * <p>{@code null} поля из DTO <b>не меняют</b> поля Entity
     * (благодаря {@link NullValuePropertyMappingStrategy#IGNORE}).
     *
     * @param dto    входящий DTO
     * @param entity сущность для обновления
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakUserId", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    void patchEntity(ProfileDto dto, @MappingTarget CustomerProfile entity);
}