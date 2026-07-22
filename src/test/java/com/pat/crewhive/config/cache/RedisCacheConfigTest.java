package com.pat.crewhive.config.cache;

import com.pat.crewhive.company.Company;
import com.pat.crewhive.company.CompanyType;
import com.pat.crewhive.company.UserIdAndNameAndHoursDTO;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.nio.ByteBuffer;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RedisCacheConfigTest {

    private final RedisSerializationContext.SerializationPair<Object> valuePair =
            new RedisCacheConfig().cacheConfiguration().getValueSerializationPair();

    /**
     * Spring's cache abstraction deserializes cache hits via deserialize(bytes, Object.class)
     * and then unchecked-casts to the declared return type, so the serializer must embed
     * enough type info to reconstruct the concrete class, not just a generic Map.
     */
    @Test
    void roundTripsEntityThroughObjectClassLikeSpringCacheDoes() {
        Company company = new Company();
        company.setName("Acme");
        company.setCompanyType(CompanyType.RESTAURANT);

        ByteBuffer bytes = valuePair.write(company);
        Object result = valuePair.read(bytes);

        assertThat(result).isInstanceOf(Company.class);
        Company roundTripped = (Company) result;
        assertThat(roundTripped.getName()).isEqualTo("Acme");
        assertThat(roundTripped.getCompanyType()).isEqualTo(CompanyType.RESTAURANT);
    }

    @Test
    void roundTripsListOfDtosThroughObjectClassLikeSpringCacheDoes() {
        List<UserIdAndNameAndHoursDTO> list = List.of(
                new UserIdAndNameAndHoursDTO(1L, "Mario", "Rossi", 40)
        );

        ByteBuffer bytes = valuePair.write(list);
        Object result = valuePair.read(bytes);

        assertThat(result).isInstanceOf(List.class);
        List<?> roundTripped = (List<?>) result;
        assertThat(roundTripped).hasSize(1);
        assertThat(roundTripped.get(0)).isInstanceOf(UserIdAndNameAndHoursDTO.class);
        assertThat(((UserIdAndNameAndHoursDTO) roundTripped.get(0)).getFirstName()).isEqualTo("Mario");
    }
}
