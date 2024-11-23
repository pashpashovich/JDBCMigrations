package by.innowise.migrations;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationFileReaderTest {

    @Test
    void shouldFindMigrationFiles() {
        // given
        String directoryPath = "migrations/";
        //when
        List<File> files = MigrationFileReader.getMigrationFiles(directoryPath);
        //then
        assertNotNull(files);
        assertEquals(3, files.size());
        assertTrue(files.stream().allMatch(file -> file.getName().matches("V\\d+__.*\\.sql")));
    }

    @Test
    void shouldThrowExceptionForInvalidDirectory() {
        // given
        String directoryPath = "migrations2/";
        //when,then
        assertThrows(
                IllegalArgumentException.class,
                () -> MigrationFileReader.getMigrationFiles(directoryPath)
        );
    }

    @Test
    void shouldExtractCorrectVersion() {
        // given
        File file = new File("V1__init.sql");
        //when
        String version = MigrationFileReader.extractVersion(file);
        //then
        assertEquals("1", version);
    }
}