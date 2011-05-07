import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import org.bukkit.Player;


public class ParameterManager {
	
	CommunicationManager communicationManager;
	
	protected static final Logger log = Logger.getLogger("Minecraft");
	
	String propertiesFilename;
	
	public FakeParamClass fakeHelp = new FakeParamClass();
	
	PropertiesFile pfLocal = null;
	
	ArrayList<ParameterInfo> parameters = new ArrayList<ParameterInfo>();
	
	boolean helpRegistered = false;
	
	void registerParameter( ParameterInfo parameterInfo ) {
		
		if( !helpRegistered ) {

			parameters.add(new ParameterInfo( 
						this, 
						"fakeHelp",
						"help",
						fakeHelp.getClass(),
						new Integer(2000),
						new String[] {
							"Use help to see all commands",
							"Use help <number> to see page <number> of command list",
							"Use help <command> to see detailed information"
						},
						"gives information on commands"
				)
			);
			helpRegistered = true;
		}
		
		parameters.add(parameterInfo);
		
	}
	
	synchronized void setPropertiesFilename( String propertiesFilename ) {
		
		this.propertiesFilename = propertiesFilename;
		
	}
	
	synchronized void setCommunicationManager( CommunicationManager communicationManager ) {
		this.communicationManager = communicationManager;
	}
	
	synchronized boolean processCommand( Player player , String[] split ) {
		
		if( split.length > 2 && split[1].equals("invite") ) {

			String hostname = split[2];
			String[] hostsplit = hostname.split(":",-1);

			int portnum = 25465;
			
			if( hostsplit.length > 1 ) {

				if( !MiscUtils.isInt(hostsplit[1]) ) {
					MiscUtils.safeMessage(player, "Unable to parse port number for target server");
					return true;
				} else {
					portnum = MiscUtils.getInt(hostsplit[1]);
					hostname = hostsplit[0];
				}
			} else if( split.length > 3 ) {
				if( !MiscUtils.isInt(split[3]) ) {
					MiscUtils.safeMessage(player, "Unable to parse port number for target server");
					return true;
				} else {
					portnum = MiscUtils.getInt(split[3]);
				}

			}
			communicationManager.attemptInvite( player.getName() , hostname , portnum );
			MiscUtils.safeMessage(player, "[ServerPort] Attempting to connect to " + hostname + " on port " + portnum);
			return true;
		}
		
		if( split.length < 2 || (split[1].equalsIgnoreCase("help") && split.length == 2) ) {
			
			MiscUtils.safeMessage(player, "Command list");
			
			Iterator<ParameterInfo> itr = parameters.iterator();
			
			StringBuilder sb = new StringBuilder("");
			
			while( itr.hasNext() ) {
				ParameterInfo current = itr.next();
				sb.append( ", " + current.commandName );
			}
			
			MiscUtils.safeMessage(player, sb.toString() );
			
			return true;
			
		}
		
		if( split.length > 2 && split[1].equals("help") ) {
			
			if( MiscUtils.isInt(split[2])) {
				
				MiscUtils.safeMessage(player, "");
				MiscUtils.safeMessage(player, Colors.Green + "ServerPort Command List Page " + MiscUtils.getInt(split[2]));
				MiscUtils.safeMessage(player, "");
				int cnt=0;
				int target = 8*MiscUtils.getInt(split[2])-8;
				
				Iterator<ParameterInfo> itr = parameters.iterator();
				while( itr.hasNext() && cnt < target + 8 ) {
					ParameterInfo current = itr.next();
					if( cnt>=target ) {
						MiscUtils.safeMessage(player, 
								Colors.LightBlue + current.commandName + 
								Colors.White + ": " + current.shortHelp );
					}
					cnt++;
				}
				return true;
			}
			
			
			Iterator<ParameterInfo> itr = parameters.iterator();
			
			while( itr.hasNext() ) {
				ParameterInfo current = itr.next();
				if( split[2].equalsIgnoreCase(current.commandName) ) {
					MiscUtils.safeMessage(player, current.commandName );
					MiscUtils.safeMessage(player, "" );
					for( String line : current.longHelp ) {
						MiscUtils.safeMessage( player , line );
					}
					return true;
					
				}
			}
	
		}
		
		if( split.length > 2 ) {
			
			if( parameterExists( split[1] ) ) {
				
				if( !setParameter( split[1], split[2] ) ) {
					MiscUtils.safeMessage(player, "[Serverport] Unable to set parse parameter value");
				} else {
					MiscUtils.safeMessage(player, "[Serverport] " + split[1] + " set to " + getParameter( split[1] ));
				}
				return true;
				
			} else {
				return false;
			}
			
		} else if ( split.length == 2 ) {
			if( parameterExists( split[1] ) ) {

				MiscUtils.safeMessage(player, "[Serverport] " + split[1] + " set to " + getParameter( split[1] ));
				return true;

			} else {
				return false;
			}
		}

		return false;
		
	}
	
	synchronized boolean parameterExists( String parameterName ) {

		Iterator<ParameterInfo> itr = parameters.iterator();

		while( itr.hasNext() ) {

			ParameterInfo current = itr.next();

			if( current.commandName.equals(parameterName) ) {

				return true;

			}

		}
		
		return false;

	}
	
	synchronized void loadParameters() {
		
		PropertiesFile pf = new PropertiesFile( propertiesFilename );
		
		try {
			pf.load();
		} catch (IOException ioe) {
			MiscUtils.safeLogging(log, "[ServerPort] Unable to open properties file: " + propertiesFilename );
			return;
		}
		
		Iterator<ParameterInfo> itr = parameters.iterator();
		
		while( itr.hasNext() ) {
			loadParameter(pf , itr.next());
		}
		
	}
	
	synchronized void loadParameter( PropertiesFile pf , ParameterInfo parameterInfo ) {
		
		pfLocal = pf;
		
		String fieldName = parameterInfo.fieldName;
		
		Field paramField = null;
		
		try {
			paramField = (parameterInfo.location.getClass()).getField(fieldName);
		} catch (NoSuchFieldException nsfe) {
			MiscUtils.safeLogging(log, "[ServerPort] Unable to find field: " + fieldName + " of " + parameterInfo.location.getClass().getName());
			return;
		}
		
		Class type = parameterInfo.type;
				
		try {
		if( type.equals(Integer.class)) {
			Integer value = pf.getInt( parameterInfo.commandName , (Integer)parameterInfo.defaultValue);
			paramField.set(parameterInfo.location, value);
		} else if( type.equals(Long.class)) {
			Long value = pf.getLong( parameterInfo.commandName , (Long)parameterInfo.defaultValue);
			paramField.set(parameterInfo.location, value);
		} else if( type.equals(Boolean.class)) {
			Boolean value = pf.getBoolean( parameterInfo.commandName , (Boolean)parameterInfo.defaultValue);
			paramField.set(parameterInfo.location, value);
		} else if( type.equals(Double.class)) {
			Double value = pf.getDouble( parameterInfo.commandName , (Double)parameterInfo.defaultValue);
			paramField.set(parameterInfo.location, value);
		} else if( type.equals(String.class)) {
			String value = pf.getString( parameterInfo.commandName , (String)parameterInfo.defaultValue);
			paramField.set(parameterInfo.location, value);
		} else if( type.equals(StringList.class)) {
			String value = pf.getString( parameterInfo.commandName , (String)parameterInfo.defaultValue);
			try {
				Method setValues = StringList.class.getMethod("setValues", new Class[] { String.class } );
				try {
					setValues.invoke(paramField.get(parameterInfo.location), new Object[] { value } );
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					MiscUtils.safeLogging("[ServerPort] Illegal Arguments for setValues");
				} catch (InvocationTargetException e) {
					e.printStackTrace();
					MiscUtils.safeLogging("[ServerPort] Illegal target for setValues");
				}
				
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				MiscUtils.safeLogging("[ServerPort] Unable to find setValues method");
			}
		} else if( type.equals(FakeParamClass.class)) {
			
		} else {
			MiscUtils.safeLogging(log, "[ServerPort] Unable to handle " + type.getName() + " for parameters");
		}
		} catch (IllegalAccessException iae) {
			MiscUtils.safeLogging(log, "[ServerPort] unable to update memory for parameter " + fieldName );
			
		}
		
	}
	
	synchronized boolean setParameter( String parameterName , String value ) {
		
		Iterator<ParameterInfo> itr = parameters.iterator();
		
		while( itr.hasNext() ) {
			
			ParameterInfo current = itr.next();
			
			if( current.commandName.equals(parameterName) ) {
				
				return setParameter( current , value );
				
			}
			
		}
		
		MiscUtils.safeLogging(log, "[ServerPort] attempted to set unknown paramer " + parameterName );
		return false;
	}
	
	synchronized boolean setParameter( ParameterInfo parameterInfo , String string ) {

		String fieldName = parameterInfo.fieldName;

		Field paramField = null;

		try {
			paramField = (parameterInfo.location.getClass()).getField(fieldName);
		} catch (NoSuchFieldException nsfe) {
			MiscUtils.safeLogging(log, "[ServerPort] Unable to find field: " + fieldName + " of " + parameterInfo.location.getClass().getName());
			return false;
		}

		Class type = parameterInfo.type;
		
		try {
			Object value;
			if( type.equals(Integer.class)) {
				if( !MiscUtils.isInt(string) ) {
					return false;
				} else {
					value = MiscUtils.getInt(string);
				}
				pfLocal.setInt(parameterInfo.commandName, (Integer)value);
				pfLocal.save();
				paramField.set(parameterInfo.location, (Integer)value);
				return true;
			} else if( type.equals(Boolean.class)) {
				if( !MiscUtils.isBoolean(string) ) {
					return false;
				} else {
					value = MiscUtils.getBoolean(string);
				}
				pfLocal.setBoolean(parameterInfo.commandName, (Boolean)value);
				pfLocal.save();
				paramField.set(parameterInfo.location, value);
				return true;
			} else if( type.equals(Long.class)) {
				if( !MiscUtils.isLong(string) ) {
					return false;
				} else {
					value = MiscUtils.getLong(string);
				}
				pfLocal.setLong(parameterInfo.commandName, (Long)value);
				pfLocal.save();
				paramField.set(parameterInfo.location, value);
				return true;
			} else if( type.equals(Double.class)) {
				if( !MiscUtils.isDouble(string) ) {
					return false;
				} else {
					value = MiscUtils.getDouble(string);
				}
				pfLocal.setDouble(parameterInfo.commandName, (Double)value);
				pfLocal.save();
				paramField.set(parameterInfo.location, value);
				return true;
			} else if( type.equals(String.class)) {
				value = string;
				pfLocal.setString(parameterInfo.commandName, (String)value);
				pfLocal.save();
				paramField.set(parameterInfo.location, value);
				return true;
			} else if( type.equals(StringList.class)) {
				StringList stringList = (StringList)paramField.get(parameterInfo.location);
				
				stringList.toggle(parameterInfo.commandName, string);
				
				value = stringList.toString();
				pfLocal.setString(parameterInfo.commandName, (String)value);
				pfLocal.save();
				
				return true;
			} else if( type.equals(FakeParamClass.class)) {
				return false;
			} else {
				MiscUtils.safeLogging(log, "[ServerPort] Unable to handle " + type.getName() + " for parameters");
				return false;
			}
		} catch (IllegalAccessException iae) {
			MiscUtils.safeLogging(log, "[ServerPort] unable to update memory for parameter " + fieldName );
			return false;
		}

	}
	
	
	synchronized String getParameter( String parameterName ) {
		
		Iterator<ParameterInfo> itr = parameters.iterator();
		
		while( itr.hasNext() ) {
			
			ParameterInfo current = itr.next();
			
			if( current.commandName.equals(parameterName) ) {
				
				return getParameter( current );
				
			}
			
		}
		
		MiscUtils.safeLogging(log, "[ServerPort] attempted to get unknown paramer " + parameterName );
		return "unknown";
	}
	
	synchronized String getParameter( ParameterInfo parameterInfo  ) {

		String fieldName = parameterInfo.fieldName;

		Field paramField = null;

		try {
			paramField = (parameterInfo.location.getClass()).getField(fieldName);
		} catch (NoSuchFieldException nsfe) {
			MiscUtils.safeLogging(log, "[ServerPort] Unable to find field: " + fieldName + " of " + parameterInfo.location.getClass().getName());
			return "unable to find field";
		}
		
		try {
			return paramField.get(parameterInfo.location).toString();
		} catch (IllegalAccessException iae) {
			MiscUtils.safeLogging(log, "[ServerPort] unable to update memory for parameter " + fieldName );
			return "unable to access memory for parameter";
		}

	}

	
	

}
