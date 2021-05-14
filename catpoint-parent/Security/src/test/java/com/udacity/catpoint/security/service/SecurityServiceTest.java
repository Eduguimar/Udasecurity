package com.udacity.catpoint.security.service;


import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    private SecurityService securityService;

    private Sensor sensor;

    private final String random = UUID.randomUUID().toString();

    @Mock
    private StatusListener statusListener;

    @Mock
    private ImageService imageService;

    @Mock
    private SecurityRepository securityRepository;

    private Sensor getNewSensor() {
        return new Sensor(random, SensorType.DOOR);
    }

    private Set<Sensor> getAllSensors(int count, boolean status) {
        Set<Sensor> sensors = new HashSet<>();
         for (int i = 0; i < count; i++) {
             sensors.add(new Sensor(random, SensorType.DOOR));
         }
         sensors.forEach(sensor -> sensor.setActive(status));

         return sensors;
    }

    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, imageService);
        sensor = getNewSensor();
    }

    // Test 1
    @Test
    void ifSystemArmedAndSensorActivated_changeStatusToPending() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }
    // Test 2
    @Test
    void ifSystemArmedAndSensorActivatedAndPendingState_changeStatusToAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }
    // Test 3
    @Test
    void ifPendingAlarmAndSensorInactive_returnNoAlarmState() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    // Test 4
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ifAlarmIsActive_changeSensorShouldNotAffectAlarmState(boolean status) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, status);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }
    // Test 5
    @Test
    void ifSensorActivatedWhileActiveAndPendingAlarm_changeStatusToAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }
    // Test 6
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM", "ALARM"})
    void ifSensorDeactivatedWhileInactive_noChangesToAlarmState(AlarmStatus status) {
        when(securityRepository.getAlarmStatus()).thenReturn(status);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }
    // Test 7
    @Test
    void ifImageServiceIdentifiesCatWhileAlarmArmedHome_changeStatusToAlarm() {
        BufferedImage catImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(true);
        securityService.processImage(catImage);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }
    // Test 8
    @Test
    void ifImageServiceIdentifiesNoCatImage_changeStatusToNoAlarmAsLongSensorsNotActive() {
        Set<Sensor> sensors = getAllSensors(3, false);
        when(securityRepository.getSensors()).thenReturn(sensors);
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(false);
        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    // Test 9
    @Test
    void ifSystemDisarmed_setNoAlarmState() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    // Test 10
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void ifSystemArmed_resetSensorsToInactive(ArmingStatus status) {
        Set<Sensor> sensors = getAllSensors(3, true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.setArmingStatus(status);

        securityService.getSensors().forEach(sensor -> {
            assertFalse(sensor.getActive());
        });
    }
    // Test 11
    @Test
    void ifSystemArmedHomeWhileImageServiceIdentifiesCat_changeStatusToAlarm() {
        BufferedImage catImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.processImage(catImage);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    //extra tests to cover the whole SecurityService class
    //Status Listener test
    @Test
    void addAndRemoveStatusListener() {
        securityService.addStatusListener(statusListener);
        securityService.removeStatusListener(statusListener);
    }
    //Sensor Listener test
    @Test
    void addAndRemoveSensor() {
        securityService.addSensor(sensor);
        securityService.removeSensor(sensor);
    }
    //System disarmed Test
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM"})
    void ifSystemDisarmedAndSensorActivated_noChangesToArmingState(AlarmStatus status) {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(securityRepository.getAlarmStatus()).thenReturn(status);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, never()).setArmingStatus(ArmingStatus.DISARMED);
    }
    //System deactivated and Alarm state test
    @Test
    void ifAlarmStateAndSystemDisarmed_changeStatusToPending() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }
}