package com.cenerino.jmxext;

import static org.hamcrest.CoreMatchers.is;
import static org.jboss.shrinkwrap.api.asset.EmptyAsset.INSTANCE;
import static org.junit.Assert.assertThat;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class JmxExtensionTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
                .addPackages(true, "com.cenerino.jmxext")
                .addAsManifestResource(INSTANCE, "beans.xml");
    }

    @Test
    public void shouldExposeAnnotatedClass() {
        // TODO to be implemented
        assertThat(true, is(true));
    }
}
