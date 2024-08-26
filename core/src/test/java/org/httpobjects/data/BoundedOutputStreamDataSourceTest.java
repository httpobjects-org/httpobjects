package org.httpobjects.data;

public class BoundedOutputStreamDataSourceTest extends DataSourceTest{

    @Override
    DataSource createTestSubject(byte[] data) {
        return new OutputStreamDataSource((out, limit) -> {
            try {
                if(limit==null){
                    out.write(data);
                }else{
                    out.write(data, 0, limit.intValue());
                }
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        });
    }
}
