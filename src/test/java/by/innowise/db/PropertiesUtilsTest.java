package by.innowise.db;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PropertiesUtilsTest {

    @Test
    void shouldLoadPropertiesFromMockedInputStream() throws IOException {
        // given
        String mockProperties = "db.url=jdbc:h2:mem:testdb\ndb.username=sa\ndb.password=";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(mockProperties.getBytes());
        //when
        Properties properties = new Properties();
        properties.load(inputStream);
        //then
        assertEquals("jdbc:h2:mem:testdb", properties.getProperty("db.url"));
        assertEquals("sa", properties.getProperty("db.username"));
        assertEquals("",properties.getProperty("db.password"));
    }

    @Test
    void shouldReturnNullForMissingProperty() {
        // given
        Properties properties = new Properties();
        properties.setProperty("db.url", "jdbc:h2:mem:testdb");
        //when,then
        assertEquals("jdbc:h2:mem:testdb", properties.getProperty("db.url"));
        assertNull(properties.getProperty("db.username"));
    }
}
