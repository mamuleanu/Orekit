/* Copyright 2002-2012 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.frames;


import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.data.AbstractFilesLoaderTest;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;


public class BulletinBFilesLoaderTest extends AbstractFilesLoaderTest {

    @Test
    public void testMissingMonths() throws OrekitException {
        setRoot("missing-months");
        EOP2000History history = new EOP2000History();
        new BulletinBFilesLoader(FramesFactory.BULLETINB_2000_FILENAME).fillHistory(history);
        Assert.assertTrue(getMaxGap(history) > 5);
    }

    @Test
    public void testStartDate() throws OrekitException, ParseException {
        setRoot("regular-data");
        EOP2000History history = new EOP2000History();
        new BulletinBFilesLoader(FramesFactory.BULLETINB_2000_FILENAME).fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(2005, 12, 5, TimeScalesFactory.getUTC()),
                            history.getStartDate());
    }

    @Test
    public void testEndDate() throws OrekitException, ParseException {
        setRoot("regular-data");
        EOP2000History history = new EOP2000History();
        new BulletinBFilesLoader(FramesFactory.BULLETINB_2000_FILENAME).fillHistory(history);
        Assert.assertTrue(getMaxGap(history) < 5);
        Assert.assertEquals(new AbsoluteDate(2006, 3, 5, TimeScalesFactory.getUTC()),
                            history.getEndDate());
    }

    @Test
    public void testNewFormatNominal() throws OrekitException, ParseException {
        setRoot("new-bulletinB");
        EOP1980History history = new EOP1980History();
        new BulletinBFilesLoader("bulletinb.270").fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(2010, 6, 2, TimeScalesFactory.getUTC()),
                            history.getStartDate());
        Assert.assertEquals(new AbsoluteDate(2010, 7, 1, TimeScalesFactory.getUTC()),
                            history.getEndDate());
    }

    @Test
    public void testOldFormatContent() throws OrekitException, ParseException {
        setRoot("regular-data");
        EOP2000History history = new EOP2000History();
        new BulletinBFilesLoader(FramesFactory.BULLETINB_2000_FILENAME).fillHistory(history);
        AbsoluteDate date = new AbsoluteDate(2006, 1, 11, 12, 0, 0, TimeScalesFactory.getUTC());
        Assert.assertEquals(msToS(  (-3 * 0.073    + 27 * -0.130   + 27 * -0.244   - 3 * -0.264)   / 48), history.getLOD(date), 1.0e-10);
        Assert.assertEquals(        (-3 * 0.333275 + 27 * 0.333310 + 27 * 0.333506 - 3 * 0.333768) / 48,  history.getUT1MinusUTC(date), 1.0e-10);
        Assert.assertEquals(asToRad((-3 * 0.04958  + 27 * 0.04927  + 27 * 0.04876  - 3 * 0.04854)  / 48), history.getPoleCorrection(date).getXp(), 1.0e-10);
        Assert.assertEquals(asToRad((-3 * 0.38117  + 27 * 0.38105  + 27 * 0.38071  - 3 * 0.38036)  / 48), history.getPoleCorrection(date).getYp(), 1.0e-10);
    }

    @Test
    public void testNewFormatContent() throws OrekitException, ParseException {
        setRoot("new-bulletinB");
        EOP1980History history = new EOP1980History();
        new BulletinBFilesLoader("bulletinb.270").fillHistory(history);
        AbsoluteDate date = new AbsoluteDate(2010, 6, 12, 12, 0, 0, TimeScalesFactory.getUTC());
        Assert.assertEquals(msToS((   -3 *   0.1202 + 27 *   0.0294 + 27 *   0.0682 - 3 *   0.1531) / 48), history.getLOD(date), 1.0e-10);
        Assert.assertEquals(msToS((   -3 * -57.1711 + 27 * -57.2523 + 27 * -57.3103 - 3 * -57.4101) / 48), history.getUT1MinusUTC(date), 1.0e-10);
        Assert.assertEquals(masToRad((-3 *  -1.216  + 27 *   1.658  + 27 *   4.926  - 3 *   7.789)  / 48), history.getPoleCorrection(date).getXp(), 1.0e-10);
        Assert.assertEquals(masToRad((-3 * 467.780  + 27 * 469.330  + 27 * 470.931  - 3 * 472.388)  / 48), history.getPoleCorrection(date).getYp(), 1.0e-10);
        Assert.assertEquals(masToRad((-3 * -64.899  + 27 * -65.018  + 27 * -65.067  - 3 * -64.998)  / 48), history.getNutationCorrection(date).getDdpsi(), 1.0e-10);
        Assert.assertEquals(masToRad((-3 *  -9.955  + 27 *  -9.927  + 27 * -10.036  - 3 * -10.146)  / 48), history.getNutationCorrection(date).getDdeps(), 1.0e-10);
    }

    private double msToS(double ms) {
        return ms / 1000.0;
    }

    private double asToRad(double mas) {
        return mas * Constants.ARC_SECONDS_TO_RADIANS;
    }

    private double masToRad(double mas) {
        return mas * Constants.ARC_SECONDS_TO_RADIANS / 1000.0;
    }

    @Test(expected=OrekitException.class)
    public void testNewFormatTruncated() throws OrekitException, ParseException {
        setRoot("new-bulletinB");
        EOP1980History history = new EOP1980History();
        new BulletinBFilesLoader("bulletinb-truncated.270").fillHistory(history);
    }

    @Test(expected=OrekitException.class)
    public void testNewFormatInconsistent() throws OrekitException, ParseException {
        setRoot("new-bulletinB");
        EOP1980History history = new EOP1980History();
        new BulletinBFilesLoader("bulletinb-inconsistent.270").fillHistory(history);
    }

}
