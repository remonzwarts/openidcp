/*
 * This work is protected under copyright law in the Kingdom of
 * The Netherlands. The rules of the Berne Convention for the
 * Protection of Literary and Artistic Works apply.
 * Digital Me B.V. is the copyright owner.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.qiy.demo.idp.dw;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.stream.Collectors;

import javax.ws.rs.core.MediaType;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.qiy.oic.op.qiy.QiyConnectTokenRepresentation;
import nl.qiy.oic.op.qiy.messagebodywriter.TemplateConnectTokenBodyWriter;

/**
 * TODO: friso should have written a comment here to tell us what this class does
 *
 * @author friso
 * @since 16 dec. 2016
 */
public class HTMLTemplateConnectTokenTest {
    /** 
     * Standard SLF4J Logger 
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(HTMLTemplateConnectTokenTest.class);

    @Test
    public void test() throws IOException {
        String template = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/index.html")))
                .lines().collect(Collectors.joining(" ")).replaceAll("\\s+", " ");
        LOGGER.info("read template: \n{}\n", template);
        TemplateConnectTokenBodyWriter.registerTemplate(template, MediaType.TEXT_HTML_TYPE);
        OutputStream stream = new ByteArrayOutputStream(20000);
        QiyConnectTokenRepresentation qctr = new QiyConnectTokenRepresentation("hallo".getBytes(), "{\"a\":true}", "http://notification.url/blaat", "dappre://connect?blaat");
        new TemplateConnectTokenBodyWriter().writeTo(qctr, QiyConnectTokenRepresentation.class, null, null,
                MediaType.TEXT_HTML_TYPE, null, stream);
        LOGGER.info("Stream: \n" + stream.toString());
        Assert.assertTrue(stream.toString().contains("{\"a\":true}"));

    }
}
