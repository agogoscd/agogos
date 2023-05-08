package com.redhat.agogos.cli.commands;

import com.redhat.agogos.v1alpha1.Component;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

class ListCommand extends AbstractListCommand<Component> {
}

@QuarkusTest
public class AbstractListCommandTest {

    @Test
    void byCreationTimeComparatorReturnsZeroWhenTimestampsAreTheSame() {
        String creationTimestamp = ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        Comparator<Component> byCreationTimeComparator = new ListCommand().byCreationTime();

        Component componentA = new Component("A");
        componentA.getMetadata().setCreationTimestamp(creationTimestamp);
        Component componentB = new Component("B");
        componentB.getMetadata().setCreationTimestamp(creationTimestamp);

        int returnCode = byCreationTimeComparator.compare(componentA, componentB);

        Assertions.assertTrue(returnCode == 0, "Comparator did not return 0 for same creationTime components.");
    }
}
