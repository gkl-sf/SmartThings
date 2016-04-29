/*
  Cooper Aspire RF9540-N device handler by gkl_sf

  based on SmartThings Dimmer Switch Copyright 2015 SmartThings
  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
  in compliance with the License.
  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
  for the specific language governing permissions and limitations under the License.
 
  *** Features: 
  - Device parameters configuration
  - Panic Mode (Alarm) capability
  - Protection Mode (Child Lock) capability

  *** Device configuration available to change in Preferences:
  (When preferences are updated, the lights will blink, unless already at 99%.)
   
  Parameter          Description          Range                Factory Default
 
  1                  Delayed OFF time     0 to 255 seconds     10
  2                  Panic ON time        0 to 255 seconds     1 (1 to 255 allowed by this DH)
  3                  Panic OFF time       0 to 255 seconds     1 (1 to 255 allowed by this DH)
  4 (not included)   Basic set value      0 to 255             0
  5 (not included)   Power up state       1=OFF 2=ON 3=Last    1
  6 (not included)   Panic mode enable    0=OFF 1=ON           0 (set to 1 by this DH at configure() and updated())
  7                  Dimmer ramp time     0 to 255 seconds     3
  8                  Kickstart feature    0=OFF 1=ON           1
  9 (not included)   Reset max/min levels to factory
  11                 Min dimming level    4 to 99              4 (4 to 86 allowed by this DH)
  12                 Max dimming level    4 to 99             99 (17 to 99 allowed by this DH)
 
  Manufacturer notes:
 
  Parameters 11 and 12: These values are not normally used except for possible technical support troubleshooting
                        in the case of attempting to set the dimming level to determine solutions to lamp
                        incompatibility issues. The minimum level must always be at least 13 below the maximum level.
 
  Parameter 4: Normally this parameter should be left at factory default unless specific associated devices
               require a fixed value. It is included to ensure Z-Wave certification requirements. If a value
               other than 0 is configured, then the device will send the configured value rather than the actual
               value of the dim level to associated devices. Changing this value from 0 will result in undesired
               operation in most cases.
          
 *** Alarm capability using built-in Panic Mode (flashes lights on/off)
 
  - Panic Mode must be enabled, thus, Parameter 6 will is set to 1 by this DH when preferences are updated
    Duration of ON and of OFF flashing can be changed in preferences (default is 1 second)
    siren() strobe() and both() all trigger the Panic Mode
  - When off() called, checks if Panic Mode is on, then either turns OFF Panic or switch, as appopriate
  - When physical switch is turned on or dimmer level is changed, switch will turn off Panic Mode, so this DH will update
    the Alarm state to OFF. Panic Mode will also be turned OFF if dimmer level physically changed while switch is OFF, but this is
    not visible to SmartThings, so Alarm state will still be reported as Strobe.    
  - Protection Mode does no prevent the physical switch from turning off the Panic Mode (switch design)

 *** Protection Mode -- Child Protection per manufacturer:
 
    Child protection is a way to prevent local control of an Aspire RF switch, dimmer or receptacle. There are three settings:
    - No protections allows the device to work normally
    - Sequence Control requires the user to tap the device three times to make it work
    - Remote Only completely locks out the face of the device (but Panic Mode will still be turned off with switch press)
 
    - Can be changed in preferences
    - Can be changed programatically with protectionMode(Short) where 0 is Disabled, 1 is Sequence Control, and 2 is Remote Only
    - When changed programatically, preferences are not updated (SmartThings limitation)    
    
 *** Version history
 
   28 Apr 2016  v1  initial release
 
 */
 
metadata {
	definition (name: "RF9540", namespace: "gkl-sf", author: "gkl_sf") {
    
		capability "Switch Level" //supports v2 so can accept setLevel(dimming level in %, duration in sec)
		capability "Actuator"
		capability "Switch"
        capability "Sensor"
		capability "Polling"
		capability "Refresh"
        capability "Alarm"
        
        command "protectionMode", ["string"]

        attribute "protection", "enum", ["disabled","sequenceControl","remoteOnly"]
        
		fingerprint deviceId:"0x1104", inClusters: "0x26,0x27,0x75,0x86,0x70,0x71,0x85,0x77,0x2B,0x2C,0x72,0x73,0x82,0x87"
        
    /*
    
    Command Classes:
    
    0x26: Switch Multilevel
    0x27: Switch All
    0x70: Configuration
    0x71: Alarm
    0x72: Manufacturer Specific
    0x73: Powerlevel
    0x75: Protection
    0x77: Node Naming
    0x82: Hail
    0x85: Association
    0x86: Version
    0x87: Indicator
    0x2B: Scene Activation
    0x2C: Scene Actuator Conf
    
    */
    
    }

	simulator {
		status "on":  "command: 2001, payload: FF"
		status "off": "command: 2001, payload: 00"
		status "09%": "command: 2001, payload: 09"
		status "10%": "command: 2001, payload: 0A"
		status "33%": "command: 2001, payload: 21"
		status "66%": "command: 2001, payload: 42"
		status "99%": "command: 2001, payload: 63"
        
		// reply messages
		reply "2001FF,delay 3500,2602": "command: 2603, payload: FF"
		reply "200100,delay 3500,2602": "command: 2603, payload: 00"
		reply "200119,delay 3500,2602": "command: 2603, payload: 19"
		reply "200132,delay 3500,2602": "command: 2603, payload: 32"
		reply "20014B,delay 3500,2602": "command: 2603, payload: 4B"
		reply "200163,delay 3500,2602": "command: 2603, payload: 63"
	}

    preferences {
 		input(
             "p1",
             "number",
             title: "Delayed OFF feature wait duration (default 10 sec)",
             range: "0..255",
             description: "0-255 seconds",
             defaultValue: 10,
             required: false,
             displayDuringSetup: false
             )
 		input(
             "p2",
             "number",
             title: "Panic mode flash ON duration (default 1 sec)",
             range: "1..255",
             description: "1-255 seconds",
             defaultValue: 1,
             required: false,
             displayDuringSetup: false
             )
 		input(
             "p3",
             "number",
             title: "Panic mode flash OFF duration (default 1 sec)",
             range: "1..255",
             description: "1-255 seconds",
             defaultValue: 1,
             required: false,
             displayDuringSetup: false
             )            
        input(
             "p7",
             "number",
             title: "Ramp time to reach a selected dim level (default 3 sec)",
             range: "0..255",
             description: "0-255 seconds",
             defaultValue: 3,
             required: false,
             displayDuringSetup: false
             )
 		input(
             "p8",
             "enum",
             title: "Kickstart: set lights to higher dim, then to desired level (default Enabled)",
             options: ["Enabled","Disabled"],
             defaultValue: "Enabled",
             required: false,
             displayDuringSetup: false
             )
 		input(
             "p11",
             "number",
             title: "Minimum dimming level (default 4%)",
             range: "4..86",
             description: "4-86%",
             defaultValue: 4,
             required: false,
             displayDuringSetup: false
             )
 		input(
             "p12",
             "number",
             title: "Maximum dimming level (default 99%)",
             range: "17..99",
             description: "17-99%",
             defaultValue: 99,
             required: false,
             displayDuringSetup: false
             )
        input(
             "protectionMode",
             "enum",
             title: "Protection Mode (Child Protection)",
             options: ["Disabled","Sequence Control","Remote Only"],
             defaultValue: "Disabled",
             required: false,
             displayDuringSetup: false
             ) 
    }   
    
	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.light.on", backgroundColor:"#79b821", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.light.on", backgroundColor:"#79b821", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "panic", label:'${name}', action:"alarm.off", icon:"st.security.alarm.alarm", backgroundColor:"#e86d13", nextState:"resetting"
                attributeState "resetting", label:'${name}', icon:"st.security.alarm.clear", backgroundColor:"#e86d13"                
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel"
			}
		}

		standardTile("refresh", "device.switch", width: 2, height: 2, decoration: "flat") {
			state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
		}

        standardTile("alarm", "device.alarm", width: 2, height: 2) {
            state "off", label:'${currentValue}', action:"alarm.strobe", icon:"st.security.alarm.clear", backgroundColor:"#ffffff"
			state "strobe", label:'${currentValue}', action:"alarm.off", icon:"st.security.alarm.alarm", backgroundColor:"#e86d13"
        } 
        
        valueTile("protection", "device.protection", width: 2, height: 1, decoration: "flat", wordWrap: true) {
			state "default", label:'Protection Mode\n${currentValue}'
		}
        
		main(["switch"])
		details(["switch", "refresh", "alarm", "protection"])
	}
}

def updated() {

	log.debug "updated() called"
    
    //reset to factory defaults if not otherwise set
    if (settings.p1 == null) settings.p1 = 10
    if (settings.p2 == null) settings.p2 = 1
    if (settings.p3 == null) settings.p3 = 1
    if (settings.p7 == null) settings.p7 = 3
    if (settings.p8 == null) settings.p8 = "Enabled"
    if (settings.p11 == null) settings.p11 = 4
    if (settings.p12 == null) settings.p12 = 99

    if (settings.protectionMode == null) settings.protectionMode = "Disabled"
        
/*
  Device stores parameter configuration values as a signed single byte number
  values 0 to 127 are stored as 0 to 127 (decimal) 
  values 128 to 255 are stored as -128 to -1 (negative decimal numbers) 
  as calculated by CONFIG VALUE as Byte = DESIRED VALUE - 256
*/    

    Byte param1
    if (settings.p1 > 127) param1 = settings.p1 - 256
    else param1 = settings.p1

    Byte param2
    if (settings.p2 > 127) param2 = settings.p2 - 256
    else param2 = settings.p2

    Byte param3
    if (settings.p3 > 127) param3 = settings.p3 - 256
    else param3 = settings.p3

    Byte param7
    if (settings.p7 > 127) param7 = settings.p7 - 256
    else param7 = settings.p7

    Byte param8
    if (settings.p8 == "Disabled") param8 = 0
    else param8 = 1 //factory default

    settings.p11 = Math.max(Math.min(settings.p11, 86), 4)
    settings.p12 = Math.max(Math.min(settings.p12, 99), 17)
    if ((settings.p12 - settings.p11) < 13) {
       settings.p12 = 99
    }
    if ((settings.p12 - settings.p11) < 13) {
       settings.p11 = 4
    }

    Short protectionSet
    if (settings.protectionMode == "Sequence Control") protectionSet = 1
    else if (settings.protectionMode == "Remote Only") protectionSet = 2
    else protectionSet = 0
    
    //set delay to refresh status to ramp time in msec + 500 msec
    state.delayStatus = (settings.p7 * 1000) + 500
    
    //when params updated, switch turn on to 99%, so need to return it to previous state
    def previousSwitch = device.currentValue("switch")
    def previousLevel = device.currentValue("level")
    Short switchLevel
    if (previousSwitch == "off") {
      switchLevel = 0
    }
    else {
      switchLevel = previousLevel
    }
    
    def cmds = []
    
    cmds << zwave.configurationV1.configurationSet(parameterNumber: 1, size: 1, configurationValue: [param1]).format()
    cmds << zwave.configurationV1.configurationSet(parameterNumber: 2, size: 1, configurationValue: [param2]).format()
    cmds << zwave.configurationV1.configurationSet(parameterNumber: 3, size: 1, configurationValue: [param3]).format()
    cmds << zwave.configurationV1.configurationSet(parameterNumber: 6, size: 1, configurationValue: [1]).format() //panic mode enabled
    cmds << zwave.configurationV1.configurationSet(parameterNumber: 7, size: 1, configurationValue: [param7]).format()
    cmds << zwave.configurationV1.configurationSet(parameterNumber: 8, size: 1, configurationValue: [param8]).format()
    cmds << zwave.configurationV1.configurationSet(parameterNumber: 11, size: 1, configurationValue: [settings.p11]).format()
    cmds << zwave.configurationV1.configurationSet(parameterNumber: 12, size: 1, configurationValue: [settings.p12]).format()
    cmds << protectionMode(protectionSet)
    cmds << "delay 1000"
    cmds << setLevel(switchLevel,1)
    
	response(delayBetween(cmds))
}

def parse(String description) {
	def result = null
    def cmd = zwave.parse(description, [0x20: 1, 0x26: 1, 0x70: 1, 0x72: 1, 0x75: 1])
	if (cmd) {
         result = zwaveEvent(cmd)
	}
    log.debug "PARSE\ndescription = ${description}\nparsed = ${cmd}\nresult = ${result}"
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {        
	log.debug "zwaveEvent BasicReport cmd = $cmd"
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) { //physical device events ON/OFF
    def result = []
    if (state.alarmOn) {
        log.debug "BasicSet alarm=off"
        state.alarmOn = 0
        result << createEvent(name: "alarm", value: "off", descriptionText: "Panic Mode set to OFF")
    }
    if (cmd.value) {
      log.debug "BasicSet switch=on level=${cmd.value}"       
      result << createEvent(name: "switch", value: "on", descriptionText: "Physical switch turned ON at ${cmd.value}%")
      result << createEvent(name: "level", value: cmd.value, displayed: false)
    }
    else {
      log.debug "BasicSet switch=off"           
      result << createEvent(name: "switch", value: "off", descriptionText: "Physical switch turned OFF")
    }
    result
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelReport cmd) {
    def result = []
    
    if (state.alarmOn) {
       log.debug "SwitchMultilevelReport alarm=off"              
       state.alarmOn = 0
       response(zwave.alarmV1.alarmReport(alarmType: 1, alarmLevel: 0).format())       
       result << createEvent(name: "alarm", value: "off", descriptionText: "Panic Mode set to OFF")
    }

	if (cmd.value) {
          log.debug "SwitchMultilevelReport switch=on level=${cmd.value}"       
          result << createEvent(name: "switch", value: "on", descriptionText: "Switch set to ON")
		  result << createEvent(name: "level", value: cmd.value, unit: "%", descriptionText: "Level set to ${cmd.value}%")
    }
    else {
          log.debug "SwitchMultilevelReport switch=off"       
          result << createEvent(name: "switch", value: "off", descriptionText: "Switch set to OFF")
    }
    
    result
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelSet cmd) { //physical device Level set events
    log.debug "SwitchMultilevelSet switch=on level=${cmd.value}"       
    def result = []
    result << createEvent(name: "switch", value: "on", displayed: false)
	result << createEvent(name: "level", value: cmd.value, unit: "%", descriptionText: "Physical switch level set to ${cmd.value}%")
    result   
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelStartLevelChange cmd) { //physical device event
    if (state.alarmOn) {
       state.alarmOn = 0
       createEvent(name: "alarm", value: "off", descriptionText: "Panic Mode set to OFF")
    }
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd) {
    log.debug "zwaveEvent ConfigurationReport parameter: ${cmd.parameterNumber}, value: ${cmd.configurationValue}"
}

def zwaveEvent(physicalgraph.zwave.commands.protectionv1.ProtectionReport cmd) {    
    if (cmd.protectionState == 0) {
       createEvent(name: "protection", value: "Disabled", descriptionText: "Protection Mode Disabled")
    }
    else if (cmd.protectionState == 1) {
       createEvent(name: "protection", value: "Sequence Control", descriptionText: "Protection Mode set to Sequence Control")
    }
    else if (cmd.protectionState == 2) {
       createEvent(name: "protection", value: "Remote Only", descriptionText: "Protection Mode set to Remote Only")
    }
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	[:]
    log.debug "zwaveEvent unhandled cmd = $cmd"
}

def on() {
    log.debug "on() called"
    
    sendEvent(name: "switch", value: "turningOn", displayed: false)

	delayBetween([
			zwave.basicV1.basicSet(value: 0xFF).format(),
			zwave.switchMultilevelV1.switchMultilevelGet().format()
	],state.delayStatus)
}

def off() {
    
    if (state.alarmOn) {
       log.debug "off() called --- turning off alarm"
       sendEvent(name: "switch", value: "resetting", displayed: false)
       delayBetween([
             zwave.alarmV1.alarmReport(alarmType: 1, alarmLevel: 0).format(),
             zwave.switchMultilevelV1.switchMultilevelGet().format()
       ],state.delayStatus)
    }

    else {
       log.debug "off() called --- turning off switch"
       sendEvent(name: "switch", value: "turningOff", displayed: false)
	   delayBetween([
			zwave.basicV1.basicSet(value: 0x00).format(),
			zwave.switchMultilevelV1.switchMultilevelGet().format()
	   ],state.delayStatus)    
    }
}

def setLevel(value) {
	log.debug "setLevel() called with value = ${value}"
        
    def currentSwitchState = device.currentValue("switch")
    if (currentSwitchState == "off" && value > 0) {
      sendEvent(name: "switch", value: "turningOn", displayed: false)
      }
    else if (currentSwitchState == "on" && value == 0) {
      sendEvent(name: "switch", value: "turningOff", displayed: false)
      }
    else {
      sendEvent(name: "switch", value: "dimming", displayed: false)
      } 
    
    def valueaux = value as Integer
	def level = Math.max(Math.min(valueaux, 99), 0)
        
	delayBetween ([
         zwave.basicV1.basicSet(value: level).format(),
         zwave.switchMultilevelV1.switchMultilevelGet().format()
    ],state.delayStatus)
}

def setLevel(value, duration) {
	log.debug "setLevel() called with value = ${value} and duration = ${duration}"
        
    def currentSwitchState = device.currentValue("switch")
    if (currentSwitchState == "off" && value > 0) {
      sendEvent(name: "switch", value: "turningOn", displayed: false)
      }
    else if (currentSwitchState == "on" && value == 0) {
      sendEvent(name: "switch", value: "turningOff", displayed: false)
      }
      
	def valueaux = value as Integer
	def level = Math.max(Math.min(valueaux, 99), 0)    
    def dimmingDuration = Math.max(Math.min(duration, 3600), 1)
	Long getStatusDelay = 500 + (duration*1000)
	delayBetween ([
       zwave.switchMultilevelV2.switchMultilevelSet(value: level, dimmingDuration: dimmingDuration).format(),
       zwave.switchMultilevelV1.switchMultilevelGet().format()
    ], getStatusDelay)
}

def poll() {
	zwave.switchMultilevelV1.switchMultilevelGet().format()
}

def refresh() {
	log.debug "refresh() called"
    def cmds = []
    if (state.alarmOn) {
      cmds << zwave.alarmV1.alarmReport(alarmType: 1, alarmLevel: 0).format()
    }
    cmds << zwave.switchMultilevelV1.switchMultilevelGet().format()
    delayBetween(cmds)    
}

// ALARM COMMANDS

def strobe() {
	log.debug "strobe() called"
	state.alarmOn = 1
    def cmds = []
    cmds << zwave.alarmV1.alarmReport(alarmType: 1, alarmLevel: 255).format()
    cmds << sendEvent(name: "alarm", value: "strobe", descriptionText: "Panic Mode set to ON")
    cmds << sendEvent(name: "switch", value: "panic", displayed: false)
	delayBetween(cmds)      
}

def siren() {
    log.debug "siren() called"
	strobe()
}

def both() {
    log.debug "both() called"
	strobe()
}

// PROTECTION COMMAND

def protectionMode(protectionSet) {
    log.debug "protectionMode() called with ${protectionSet}"
    def newMode = protectionSet as Short
    if (newMode == 0 || newMode == 1 || newMode == 2) {
       delayBetween([
            zwave.protectionV1.protectionSet(protectionState: protectionSet).format(),
            zwave.protectionV1.protectionGet().format()
       ],100)
    }
} 
