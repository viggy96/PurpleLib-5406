// Copyright (c) LASA Robotics and other contributors
// Open Source Software; you can modify and/or share it under the terms of
// the MIT license file in the root directory of this project.

package org.lasarobotics.drive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.lasarobotics.drive.MAXSwerveModule.GearRatio;
import org.lasarobotics.drive.MAXSwerveModule.ModuleLocation;
import org.lasarobotics.hardware.revrobotics.Spark;
import org.lasarobotics.hardware.revrobotics.Spark.MotorKind;
import org.lasarobotics.hardware.revrobotics.SparkInputsAutoLogged;
import org.lasarobotics.utils.GlobalConstants;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentMatchers;

import com.revrobotics.CANSparkBase.ControlType;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.units.Current;
import edu.wpi.first.units.Distance;
import edu.wpi.first.units.Measure;
import edu.wpi.first.units.Time;
import edu.wpi.first.units.Units;
import edu.wpi.first.units.Velocity;
import edu.wpi.first.wpilibj.Timer;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MAXSwerveModuleTest {
  private final double DELTA = 1e-3;
  private final Rotation2d ROTATION_PI = Rotation2d.fromRadians(Math.PI);

  private final GearRatio GEAR_RATIO = MAXSwerveModule.GearRatio.L3;
  private final double SLIP_RATIO = 0.08;
  private final Measure<Distance> WHEELBASE = Units.Meters.of(0.6);
  private final Measure<Distance> TRACK_WIDTH = Units.Meters.of(0.6);
  private final Measure<Time> AUTO_LOCK_TIME = Units.Seconds.of(3.0);
  private final Measure<Time> MAX_SLIPPING_TIME = Units.Seconds.of(0.6);
  private final Measure<Current> DRIVE_CURRENT_LIMIT = Units.Amps.of(50.0);

  private final Measure<Velocity<Distance>> NEO_MAX_LINEAR_SPEED = Units.MetersPerSecond.of(4.327);
  private final Measure<Velocity<Distance>> VORTEX_MAX_LINEAR_SPEED = Units.MetersPerSecond.of(5.172);

  private Spark m_lFrontDriveMotor, m_lFrontRotateMotor;
  private Spark m_rFrontDriveMotor, m_rFrontRotateMotor;
  private Spark m_lRearDriveMotor, m_lRearRotateMotor;
  private Spark m_rRearDriveMotor, m_rRearRotateMotor;

  private MAXSwerveModule m_lFrontModule;
  private MAXSwerveModule m_rFrontModule;
  private MAXSwerveModule m_lRearModule;
  private MAXSwerveModule m_rRearModule;


  @BeforeEach
  public void setup() {
    // Create mock hardware devices
    m_lFrontDriveMotor = mock(Spark.class);
    m_lFrontRotateMotor = mock(Spark.class);
    m_rFrontDriveMotor = mock(Spark.class);
    m_rFrontRotateMotor = mock(Spark.class);
    m_lRearDriveMotor = mock(Spark.class);
    m_lRearRotateMotor = mock(Spark.class);
    m_rRearDriveMotor = mock(Spark.class);
    m_rRearRotateMotor = mock(Spark.class);

    // Hardcode motor kind
    when(m_lFrontDriveMotor.getKind()).thenReturn(MotorKind.NEO);
    when(m_rFrontDriveMotor.getKind()).thenReturn(MotorKind.NEO);
    when(m_lRearDriveMotor.getKind()).thenReturn(MotorKind.NEO_VORTEX);
    when(m_rRearDriveMotor.getKind()).thenReturn(MotorKind.NEO_VORTEX);

    // Hardcode sample ID
    Spark.ID id = new Spark.ID("moduleName", 0);
    when(m_lFrontDriveMotor.getID()).thenReturn(id);
    when(m_rFrontDriveMotor.getID()).thenReturn(id);
    when(m_lRearDriveMotor.getID()).thenReturn(id);
    when(m_rRearDriveMotor.getID()).thenReturn(id);

    // Create hardware objects using mock devices
    m_lFrontModule = new MAXSwerveModule(
      new MAXSwerveModule.Hardware(m_lFrontDriveMotor, m_lFrontRotateMotor),
      MAXSwerveModule.ModuleLocation.LeftFront,
      GEAR_RATIO,
      WHEELBASE,
      TRACK_WIDTH,
      AUTO_LOCK_TIME,
      MAX_SLIPPING_TIME,
      DRIVE_CURRENT_LIMIT,
      SLIP_RATIO
    );
    m_rFrontModule = new MAXSwerveModule(
      new MAXSwerveModule.Hardware(m_rFrontDriveMotor, m_rFrontRotateMotor),
      MAXSwerveModule.ModuleLocation.RightFront,
      GEAR_RATIO,
      WHEELBASE,
      TRACK_WIDTH,
      AUTO_LOCK_TIME,
      MAX_SLIPPING_TIME,
      DRIVE_CURRENT_LIMIT,
      SLIP_RATIO
    );
    m_lRearModule = new MAXSwerveModule(
     new MAXSwerveModule.Hardware(m_lRearDriveMotor, m_lRearRotateMotor),
      MAXSwerveModule.ModuleLocation.LeftRear,
      GEAR_RATIO,
      WHEELBASE,
      TRACK_WIDTH,
      AUTO_LOCK_TIME,
      MAX_SLIPPING_TIME,
      DRIVE_CURRENT_LIMIT,
      SLIP_RATIO
    );
    m_rRearModule = new MAXSwerveModule(
      new MAXSwerveModule.Hardware(m_rRearDriveMotor, m_rRearRotateMotor),
      MAXSwerveModule.ModuleLocation.RightRear,
      GEAR_RATIO,
      WHEELBASE,
      TRACK_WIDTH,
      AUTO_LOCK_TIME,
      MAX_SLIPPING_TIME,
      DRIVE_CURRENT_LIMIT,
      SLIP_RATIO
    );

    // Disable traction control for unit tests
    m_lFrontModule.disableTractionControl();
    m_rFrontModule.disableTractionControl();
    m_lRearModule.disableTractionControl();
    m_rRearModule.disableTractionControl();
  }

  @Test
  @Order(1)
  @DisplayName("Test if module location is set correctly")
  public void moduleLocation() {
    // Check if all module locations are set
    assertEquals(new Translation2d(+WHEELBASE.in(Units.Meters) / 2, +TRACK_WIDTH.in(Units.Meters) / 2), m_lFrontModule.getModuleCoordinate());
    assertEquals(new Translation2d(+WHEELBASE.in(Units.Meters) / 2, -TRACK_WIDTH.in(Units.Meters) / 2), m_rFrontModule.getModuleCoordinate());
    assertEquals(new Translation2d(-WHEELBASE.in(Units.Meters) / 2, +TRACK_WIDTH.in(Units.Meters) / 2), m_lRearModule.getModuleCoordinate());
    assertEquals(new Translation2d(-WHEELBASE.in(Units.Meters) / 2, -TRACK_WIDTH.in(Units.Meters) / 2), m_rRearModule.getModuleCoordinate());
  }

  @Test
  @Order(2)
  @DisplayName("Test if module can set state")
  public void set() {
    // Hardcode sensor values
    SparkInputsAutoLogged sparkInputs = new SparkInputsAutoLogged();
    when(m_lFrontRotateMotor.getInputs()).thenReturn(sparkInputs);
    when(m_rFrontRotateMotor.getInputs()).thenReturn(sparkInputs);
    when(m_lRearRotateMotor.getInputs()).thenReturn(sparkInputs);
    when(m_rRearRotateMotor.getInputs()).thenReturn(sparkInputs);

    // Try to set module state
    SwerveModuleState state = new SwerveModuleState(+2.0, Rotation2d.fromRadians(+Math.PI));
    m_lFrontModule.set(state);
    m_rFrontModule.set(state);
    m_lRearModule.set(state);
    m_rRearModule.set(state);

    // Verify that motors are being driven with expected values
    verify(m_lFrontDriveMotor, times(1)).set(AdditionalMatchers.eq(+2.0, DELTA), ArgumentMatchers.eq(ControlType.kVelocity));
    verify(m_lFrontRotateMotor, times(1)).set(AdditionalMatchers.eq(+Math.PI / 2, DELTA), ArgumentMatchers.eq(ControlType.kPosition));
    verify(m_rFrontDriveMotor, times(1)).set(AdditionalMatchers.eq(-2.0, DELTA), ArgumentMatchers.eq(ControlType.kVelocity));
    verify(m_rFrontRotateMotor, times(1)).set(AdditionalMatchers.eq(0.0, DELTA), ArgumentMatchers.eq(ControlType.kPosition));
    verify(m_lRearDriveMotor, times(1)).set(AdditionalMatchers.eq(+2.0, DELTA), ArgumentMatchers.eq(ControlType.kVelocity));
    verify(m_lRearRotateMotor, times(1)).set(AdditionalMatchers.eq(0.0, DELTA), ArgumentMatchers.eq(ControlType.kPosition));
    verify(m_rRearDriveMotor, times(1)).set(AdditionalMatchers.eq(-2.0, DELTA), ArgumentMatchers.eq(ControlType.kVelocity));
    verify(m_rRearRotateMotor, times(1)).set(AdditionalMatchers.eq(+Math.PI / 2, DELTA), ArgumentMatchers.eq(ControlType.kPosition));
  }

  @Test
  @Order(3)
  @DisplayName("Test if module will auto-lock")
  public void autoLock() {
    // Hardcode sensor values
    SparkInputsAutoLogged sparkInputs = new SparkInputsAutoLogged();
    when(m_lFrontRotateMotor.getInputs()).thenReturn(sparkInputs);
    when(m_rFrontRotateMotor.getInputs()).thenReturn(sparkInputs);
    when(m_lRearRotateMotor.getInputs()).thenReturn(sparkInputs);
    when(m_rRearRotateMotor.getInputs()).thenReturn(sparkInputs);

    // Advance sim time
    Timer.delay(AUTO_LOCK_TIME.in(Units.Seconds));

    // Try to set module state
    SwerveModuleState state = new SwerveModuleState(0.0, Rotation2d.fromRadians(+Math.PI));
    m_lFrontModule.set(state);
    m_rFrontModule.set(state);
    m_lRearModule.set(state);
    m_rRearModule.set(state);

    // Verify that motors are being driven with expected values
    verify(m_lFrontDriveMotor, times(1)).set(AdditionalMatchers.eq(0.0, DELTA), ArgumentMatchers.eq(ControlType.kVelocity));
    verify(m_lFrontRotateMotor, times(1)).set(AdditionalMatchers.eq(+Math.PI / 4, DELTA), ArgumentMatchers.eq(ControlType.kPosition));
    verify(m_rFrontDriveMotor, times(1)).set(AdditionalMatchers.eq(0.0, DELTA), ArgumentMatchers.eq(ControlType.kVelocity));
    verify(m_rFrontRotateMotor, times(1)).set(AdditionalMatchers.eq(+Math.PI / 4, DELTA), ArgumentMatchers.eq(ControlType.kPosition));
    verify(m_lRearDriveMotor, times(1)).set(AdditionalMatchers.eq(0.0, DELTA), ArgumentMatchers.eq(ControlType.kVelocity));
    verify(m_lRearRotateMotor, times(1)).set(AdditionalMatchers.eq(+Math.PI / 4, DELTA), ArgumentMatchers.eq(ControlType.kPosition));
    verify(m_rRearDriveMotor, times(1)).set(AdditionalMatchers.eq(0.0, DELTA), ArgumentMatchers.eq(ControlType.kVelocity));
    verify(m_rRearRotateMotor, times(1)).set(AdditionalMatchers.eq(+Math.PI / 4, DELTA), ArgumentMatchers.eq(ControlType.kPosition));
  }

  @Test
  @Order(4)
  @DisplayName("Test if module works in simulation")
  public void simulation() {
    // Hardcode sensor values
    SparkInputsAutoLogged defaultInputs = new SparkInputsAutoLogged();
    SparkInputsAutoLogged lFrontRotateMotorInputs = new SparkInputsAutoLogged();
    lFrontRotateMotorInputs.absoluteEncoderPosition = ModuleLocation.LeftFront.offset.getRadians();
    when(m_lFrontDriveMotor.getInputs()).thenReturn(defaultInputs);
    when(m_lFrontRotateMotor.getInputs()).thenReturn(lFrontRotateMotorInputs);

    // Set state
    SwerveModuleState desiredState = new SwerveModuleState(2.0, Rotation2d.fromRadians(+Math.PI));
    m_lFrontModule.set(desiredState);

    // Run in simulation
    m_lFrontModule.simulationPeriodic();

    // Verify module reports expected position
    assertEquals(new SwerveModulePosition(-desiredState.speedMetersPerSecond * GlobalConstants.ROBOT_LOOP_PERIOD, desiredState.angle.minus(ROTATION_PI)), m_lFrontModule.getPosition());
  }

  @Test
  @Order(5)
  @DisplayName("Test if module calculates correct maximum linear speed")
  public void maxLinearSpeed() {
    SparkInputsAutoLogged sparkInputs = new SparkInputsAutoLogged();
    when(m_lFrontRotateMotor.getInputs()).thenReturn(sparkInputs);
    when(m_rFrontRotateMotor.getInputs()).thenReturn(sparkInputs);
    when(m_lRearRotateMotor.getInputs()).thenReturn(sparkInputs);
    when(m_rRearRotateMotor.getInputs()).thenReturn(sparkInputs);

    assertTrue(m_lFrontModule.getMaxLinearSpeed().isNear(NEO_MAX_LINEAR_SPEED, DELTA));
    assertTrue(m_rFrontModule.getMaxLinearSpeed().isNear(NEO_MAX_LINEAR_SPEED, DELTA));
    assertTrue(m_lRearModule.getMaxLinearSpeed().isNear(VORTEX_MAX_LINEAR_SPEED, DELTA));
    assertTrue(m_rRearModule.getMaxLinearSpeed().isNear(VORTEX_MAX_LINEAR_SPEED, DELTA));
  }
}
