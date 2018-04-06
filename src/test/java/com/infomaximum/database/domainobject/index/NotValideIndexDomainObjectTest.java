package com.infomaximum.database.domainobject.index;

import com.infomaximum.database.domainobject.filter.IndexFilter;
import com.infomaximum.domain.StoreFileReadable;
import com.infomaximum.database.domainobject.StoreFileDataTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by kris on 22.04.17.
 */
public class NotValideIndexDomainObjectTest extends StoreFileDataTest {

    @Test
    public void run() throws Exception {
        try {
            domainObjectSource.find(StoreFileReadable.class, new IndexFilter("zzzzz", null));
            Assert.fail();
        } catch (Exception ignore) {}

        try {
            domainObjectSource.find(StoreFileReadable.class, new IndexFilter("xxxxx", null).appendField("yyyyy", null));
            Assert.fail();
        } catch (Exception ignore) {}
    }
}