/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.scrplugin.ant;


import org.apache.felix.scrplugin.Log;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;


public class AntLog implements Log
{

    private final Task task;


    AntLog( final Task task )
    {
        this.task = task;
    }


    public boolean isDebugEnabled()
    {
        // cannot tell, assume yes
        return true;
    }


    public void debug( String content )
    {
        task.log( content, Project.MSG_DEBUG );
    }


    public void debug( String content, Throwable error )
    {
        task.log( content, error, Project.MSG_DEBUG );
    }


    public void debug( Throwable error )
    {
        task.log( error, Project.MSG_DEBUG );
    }


    public boolean isInfoEnabled()
    {
        // cannot tell, assume yes
        return true;
    }


    public void info( String content )
    {
        task.log( content, Project.MSG_INFO );
    }


    public void info( String content, Throwable error )
    {
        task.log( content, error, Project.MSG_INFO );
    }


    public void info( Throwable error )
    {
        task.log( error, Project.MSG_INFO );
    }


    public boolean isWarnEnabled()
    {
        // cannot tell, assume yes
        return true;
    }


    public void warn( String content )
    {
        task.log( content, Project.MSG_WARN );
    }


    public void warn( String content, String location, int lineNumber )
    {
        warn( String.format( "%s [%s,%d]", content, location, lineNumber ) );
    }

    public void warn( String content, String location, int lineNumber, int columNumber )
    {
    	warn( String.format( "%s [%s,%d:%d]", content, location, lineNumber , columNumber) );
    }


    public void warn( String content, Throwable error )
    {
        task.log( content, error, Project.MSG_WARN );
    }


    public void warn( Throwable error )
    {
        task.log( error, Project.MSG_WARN );
    }


    public boolean isErrorEnabled()
    {
        // cannot tell, assume yes
        return true;
    }


    public void error( String content )
    {
        task.log( content, Project.MSG_ERR );
    }


    public void error( String content, String location, int lineNumber )
    {
        error( String.format( "%s [%s,%d]", content, location, lineNumber ) );
    }

    public void error( String content, String location, int lineNumber, int columnNumber )
    {
    	error( String.format( "%s [%s,%d:%d]", content, location, lineNumber, columnNumber ) );
    }
    

    public void error( String content, Throwable error )
    {
        task.log( content, error, Project.MSG_ERR );
    }


    public void error( Throwable error )
    {
        task.log( error, Project.MSG_ERR );
    }

}
