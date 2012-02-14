/* Copyright 2002-2011 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.forces;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.propagation.SpacecraftState;

/** This class represents the features of a simplified spacecraft.
 * <p>The model of this spacecraft is a simple spherical model, this
 * means that all coefficients are constant and do not depend of
 * the direction.</p>
 * <p>Instances of this class are guaranteed to be immutable.</p>
 *
 * @see BoxAndSolarArraySpacecraft
 * @author &Eacute;douard Delente
 * @author Fabien Maussion
 * @author Pascal Parraud
 */
public class SphericalSpacecraft implements RadiationSensitive, DragSensitive {

    /** Serializable UID. */
    private static final long serialVersionUID = -1596721390500187750L;

    /** Cross section (m<sup>2</sup>). */
    private final double crossSection;

    /** Drag coefficient. */
    private double dragCoeff;

    /** Absorption coefficient. */
    private double absorptionCoeff;

    /** Specular reflection coefficient. */
    private double specularReflectionCoeff;

    /** Composite drag coefficient (S.Cd/2). */
    private double kD;

    /** Composite radiation pressure coefficient. */
    private double kP;

    /** Simple constructor.
     * @param crossSection Surface (m<sup>2</sup>)
     * @param dragCoeff drag coefficient (used only for drag)
     * @param absorptionCoeff absorption coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     * @param reflectionCoeff specular reflection coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     */
    public SphericalSpacecraft(final double crossSection,
                               final double dragCoeff,
                               final double absorptionCoeff,
                               final double reflectionCoeff) {

        this.crossSection            = crossSection;
        this.dragCoeff               = dragCoeff;
        this.absorptionCoeff         = absorptionCoeff;
        this.specularReflectionCoeff = reflectionCoeff;

        this.setKD();
        this.setKP();
    }

    /** {@inheritDoc} */
    public Vector3D dragAcceleration(final SpacecraftState state, final double density,
                                     final Vector3D relativeVelocity) {
        return new Vector3D(density * relativeVelocity.getNorm() * kD / state.getMass(), relativeVelocity);
    }

    /** {@inheritDoc} */
    public Vector3D radiationPressureAcceleration(final SpacecraftState state, final Vector3D flux) {
        return new Vector3D(kP / state.getMass(), flux);
    }

    /** {@inheritDoc} */
    public void setDragCoefficient(final double value) {
        dragCoeff = value;
        this.setKD();
    }

    /** {@inheritDoc} */
    public double getDragCoefficient() {
        return dragCoeff;
    }

    /** {@inheritDoc} */
    public void setAbsorptionCoefficient(final double value) {
        absorptionCoeff = value;
        this.setKP();
    }

    /** {@inheritDoc} */
    public double getAbsorptionCoefficient() {
        return absorptionCoeff;
    }

    /** {@inheritDoc} */
    public void setReflectionCoefficient(final double value) {
        specularReflectionCoeff = value;
        this.setKP();
    }

    /** {@inheritDoc} */
    public double getReflectionCoefficient() {
        return specularReflectionCoeff;
    }

    /** Set kD value. */
    private void setKD() {
        kD = dragCoeff * crossSection / 2;
    }

    /** Set kP value. */
    private void setKP() {
        kP = crossSection * (1 + 4 * (1.0 - absorptionCoeff) * (1.0 - specularReflectionCoeff) / 9.0);
    }

    /** Computes kP derivative with respect to absorption coefficient.
     * @return kP derivative with respect to absorption coefficient*/
    private double computeDKPDCa() {
        return -4 * crossSection * (1.0 - specularReflectionCoeff) / 9;
    }

    /** Computes kP derivative with respect to reflection coefficient.
     * @return kP derivative with respect to reflection coefficient*/
    private double computeDKPDCr() {
        return -4 * crossSection * (1.0 - absorptionCoeff) / 9;
    }

    /** {@inheritDoc} */
    public void addDAccDParam(final Vector3D acceleration, final String paramName, final double[] dAccdParam)
        throws OrekitException {

        final double coefficient;
        if (paramName.equals(SolarRadiationPressure.ABSORPTION_COEFFICIENT)) {
            coefficient = computeDKPDCa() / kP;
        } else if (paramName.equals(SolarRadiationPressure.REFLECTION_COEFFICIENT)) {
            coefficient = computeDKPDCr() / kP;
        } else {
            throw OrekitException.createIllegalArgumentException(OrekitMessages.UNSUPPORTED_PARAMETER_1_2,
                                                                 paramName,
                                                                 SolarRadiationPressure.ABSORPTION_COEFFICIENT,
                                                                 SolarRadiationPressure.REFLECTION_COEFFICIENT);
        }

        dAccdParam[0] += coefficient * acceleration.getX();
        dAccdParam[1] += coefficient * acceleration.getY();
        dAccdParam[2] += coefficient * acceleration.getZ();

    }

}
