package ut.com.mabl.bamboo;

import org.junit.Test;
import com.mabl.bamboo.api.MyPluginComponent;
import com.mabl.bamboo.impl.MyPluginComponentImpl;

import static org.junit.Assert.assertEquals;

public class MyComponentUnitTest
{
    @Test
    public void testMyName()
    {
        MyPluginComponent component = new MyPluginComponentImpl(null);
        assertEquals("names do not match!", "myComponent",component.getName());
    }
}