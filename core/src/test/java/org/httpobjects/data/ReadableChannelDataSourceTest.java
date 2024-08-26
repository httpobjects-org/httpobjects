package org.httpobjects.data;

import java.io.ByteArrayInputStream;
import java.nio.channels.Channels;

public class ReadableChannelDataSourceTest extends DataSourceTest{

    @Override
    DataSource createTestSubject(byte[] data) {
        return new ReadableChannelDataSource(() -> Channels.newChannel(new ByteArrayInputStream(data)));
    }


}
