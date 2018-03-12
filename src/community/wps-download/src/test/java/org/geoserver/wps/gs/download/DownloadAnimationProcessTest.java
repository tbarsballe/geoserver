/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import it.geosolutions.imageio.utilities.ImageIOUtilities;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.kml.KMZMapOutputFormat;
import org.geotools.image.test.ImageAssert;
import org.jcodec.api.FrameGrab;
import org.jcodec.common.Demuxer;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.Format;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.scale.AWTUtil;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;

public class DownloadAnimationProcessTest extends BaseDownloadImageProcessTest {

    @Test
    public void testDescribeProcess() throws Exception {
        Document d = getAsDOM(root() + "service=wps&request=describeprocess&identifier=gs:DownloadAnimation");
        // print(d);
        assertXpathExists("//ComplexOutput/Supported/Format[MimeType='video/mp4']", d);
    }

    @Test
    public void testAnimateBmTime() throws Exception {
        String xml = IOUtils.toString(getClass().getResourceAsStream("animateBlueMarble.xml"));
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("video/mp4", response.getContentType());

        // JCodec API works off files only... 
        File testFile = new File("target/animateBmTime.mp4");
        FileUtils.writeByteArrayToFile(testFile, response.getContentAsByteArray());

        // check frames and duration
        Format f = JCodecUtil.detectFormat(testFile);
        Demuxer d = JCodecUtil.createDemuxer(f, testFile);
        DemuxerTrack vt = d.getVideoTracks().get(0);
        DemuxerTrackMeta dtm = vt.getMeta();
        assertEquals(4, dtm.getTotalFrames());
        assertEquals(8, dtm.getTotalDuration(), 0d);

        // grab frames for checking
        File source = new File("src/test/resources/org/geoserver/wps/gs/download/bm_time.zip");
        FrameGrab grabber = FrameGrab.createFrameGrab(NIOUtils.readableChannel(testFile));
        // first
        BufferedImage frame1 = AWTUtil.toBufferedImage(grabber.getNativeFrame());
        BufferedImage expected1 = grabImageFromZip(source, "world.200402.3x5400x2700.tiff");
        ImageAssert.assertEquals(expected1, frame1, 100);
        // second
        BufferedImage frame2 = AWTUtil.toBufferedImage(grabber.getNativeFrame());
        BufferedImage expected2 = grabImageFromZip(source, "world.200403.3x5400x2700.tiff");
        ImageAssert.assertEquals(expected2, frame2, 100);
        // third
        BufferedImage frame3 = AWTUtil.toBufferedImage(grabber.getNativeFrame());
        BufferedImage expected3 = grabImageFromZip(source, "world.200404.3x5400x2700.tiff");
        ImageAssert.assertEquals(expected3, frame3, 100);
        // fourth
        BufferedImage frame4 = AWTUtil.toBufferedImage(grabber.getNativeFrame());
        BufferedImage expected4 = grabImageFromZip(source, "world.200405.3x5400x2700.tiff");
        ImageAssert.assertEquals(expected4, frame4, 100);
    }

    @Test
    public void testAnimateDecoration() throws Exception {
        String xml = IOUtils.toString(getClass().getResourceAsStream("animateDecoration.xml"));
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("video/mp4", response.getContentType());

        // JCodec API works off files only... 
        File testFile = new File("target/animateWaterDecoration.mp4");
        FileUtils.writeByteArrayToFile(testFile, response.getContentAsByteArray());

        // check frames and duration
        Format f = JCodecUtil.detectFormat(testFile);
        Demuxer d = JCodecUtil.createDemuxer(f, testFile);
        DemuxerTrack vt = d.getVideoTracks().get(0);
        DemuxerTrackMeta dtm = vt.getMeta();
        assertEquals(2, dtm.getTotalFrames());
        assertEquals(2, dtm.getTotalDuration(), 0d);

        // grab first frame for test
        FrameGrab grabber = FrameGrab.createFrameGrab(NIOUtils.readableChannel(testFile));
        BufferedImage frame1 = AWTUtil.toBufferedImage(grabber.getNativeFrame());
        ImageAssert.assertEquals(new File(SAMPLES + "animateDecorateFirstFrame.png"), frame1, 100);
    }

    @Test
    public void testAnimateTimestamped() throws Exception {
        String xml = IOUtils.toString(getClass().getResourceAsStream("animateBlueMarbleTimestamped.xml"));
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        assertEquals("video/mp4", response.getContentType());

        // JCodec API works off files only... 
        File testFile = new File("target/animateTimestamped.mp4");
        FileUtils.writeByteArrayToFile(testFile, response.getContentAsByteArray());

        // check frames and duration
        Format f = JCodecUtil.detectFormat(testFile);
        Demuxer d = JCodecUtil.createDemuxer(f, testFile);
        DemuxerTrack vt = d.getVideoTracks().get(0);
        DemuxerTrackMeta dtm = vt.getMeta();
        assertEquals(4, dtm.getTotalFrames());
        assertEquals(8, dtm.getTotalDuration(), 0d);

        // grab first frame for test
        FrameGrab grabber = FrameGrab.createFrameGrab(NIOUtils.readableChannel(testFile));
        BufferedImage frame1 = AWTUtil.toBufferedImage(grabber.getNativeFrame());
        ImageAssert.assertEquals(new File(SAMPLES + "animateBlueMarbleTimestampedFrame1.png"), frame1, 100);
    }

    BufferedImage grabImageFromZip(File file, String entryName) throws IOException {
        ZipFile zipFile = new ZipFile(file);

        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.getName().equalsIgnoreCase(entryName)) {
                try (InputStream stream = zipFile.getInputStream(entry)) {
                    return ImageIO.read(stream);
                }
            }
        }

        return null;
    }

}
