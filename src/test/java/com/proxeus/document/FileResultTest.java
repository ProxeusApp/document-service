package com.proxeus.document;

import org.junit.Test;

import static org.mockito.Mockito.*;

public class FileResultTest {

    @Test
    public void release_shouldReleaseTemplate() {
        Template template = mock(Template.class);

        final FileResult fileResult = new FileResult(template);
        fileResult.release();

        verify(template, times(1)).release();
    }
}