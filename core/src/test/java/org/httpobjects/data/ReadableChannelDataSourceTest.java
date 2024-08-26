package org.httpobjects.data;

public class ReadableChannelDataSourceTest extends DataSourceTest{

    @Override
    DataSource createTestSubject(byte[] data) {
        return new OutputStreamDataSource(out -> {
            try {
                out.write(data);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        });
    }
}
