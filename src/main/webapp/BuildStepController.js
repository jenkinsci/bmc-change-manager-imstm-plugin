 	/* ************************************************************************************************************	*/
    /*																											   	*/
	/* The script is being triggered by the following cases:  													   	*/	
	/* 																												*/
	/* 1. New Item																									*/
	/* 2. Configure - the script runs several times, per each BMC build step that is included in the configuration  */
	/* 3. Add build step (BMC plugin)																				*/
	/* 																												*/
	/* It always gets the last stepid and not the current, thus a hidden entry named "visited" is being used		*/
	/* in order to determine the current build step.																*/
	/*																											   	*/
	/* ************************************************************************************************************	*/
	/* Functionality:																								*/
	/* ************************************************************************************************************	*/ 
	/* 																												*/
	/* After the page has been loaded with all the different operands it displays only the relevant operands and	*/
	/* hides irrelevant ones.																						*/
	/* 																												*/
	/* Alerts for selecting mutually exclusive fields.																*/
	/*																												*/
	/* Updates the generated JCL according to the selected fields.													*/
	/*																												*/
	/* It detects elements by ID, which is suffixed with the current build step id. 								*/
	/* For repeatable blocks the elements are identified by the current chunk and depends on the HTML structure.	*/
	/*																											   	*/
	/* ************************************************************************************************************	*/
	/* Notes:																										*/
	/* ************************************************************************************************************	*/  
	/*																											   	*/
	/* Jenkins changed their <f:entry> implementation from <table> to <div>, thus the script had to be adjusted 	*/
	/* for detecting repeatable blocks elements.																	*/
	/*																												*/
	/* ************************************************************************************************************	*/
    
		
    var createJclJobcardPart="${JOB_CARD}\n" +
					 "//DLPYLIST EXEC PGM=DLPYLIST,REGION=4M\n";
    var createJclLoadlibsPart="";	 	
	var createJclBodyPart="//DELTAPDS  DD DISP=SHR,DSN=${DELTA_PDS}\n" +               
						 "//SYSPRINT  DD  SYSOUT=*\n" +
						 "//SYSUDUMP  DD  SYSOUT=*\n" +
						 "//SYSIN     DD  *\n" +
						 "MEMBER=${DELTA_LIST_NAME}\n" +
						 "GLOBAL=${DELTA_LIST_TITLE}\n" ;	
	// VDS= and DEPENDENT= must follow the GLOBAL= record and precede the NOTES= record.
	var createJclVdsPart="";
	var createJclDepPart="";		 
	var createJclNotesPart="NOTES=${NOTES}";
	var createJclCntlPart="";		
	createJclOptOperands="";	
    var createJclEndPart= "\n/*\n" +
    					  "//\n";
    var createJclReqOperands="";
    var createJclOptOperands="";
    var checkJclCoord="";
    var checkJclMark="";    
    var dlistElement; 	// a single record in a delta list    
    var dlistElements=new Array();   // a set of records in a delta list for a single BMC build step  
    var loadLibs=new Array();   
   	
    var updateOptionalBlockCounter=0;
    var totalNumOfoperands=68;    //should match the total number of operands
    var sectionClassName;
    /* ************************************************************************ */
    /*			 																*/
	/* Override onAdd in														*/
	/* jenkins/core/src/main/resources/lib/form/repeatable/repeatable.js		*/
	/*			 																*/
	/* ************************************************************************ */   	 
   	Behaviour.specify("INPUT.repeatable-add", 'repeatable', 0, function(e) 
   		{
   			makeButton(e,function(e) 
   			{        			
   				repeatableSupport.onAdd(e.target);// original code
   				addOrRemoveChunk(e);// new code
   			});
   			e = null; // avoid memory leak
    	});
    	
   	/* ************************************************************************ */
   	/*			 																*/
	/* Override onDelete in														*/
	/* jenkins/core/src/main/resources/lib/form/repeatable/repeatable.js		*/
   	/*			 																*/
	/* ************************************************************************ */  
   	Behaviour.specify("INPUT.repeatable-delete", 'repeatable', 0, function(e) 
   		{
   			makeButton(e,function(e)
   			{        			
   				repeatableSupport.onDelete(e.target);// original code
   				addOrRemoveChunk(e);// new code
        	});
   			e = null; // avoid memory leak
    	});

   	Behaviour.specify("BUTTON.repeatable-delete, INPUT.repeatable-delete", 'repeatable', 1, function(e) {
            e.addEventListener("click", function() {
                repeatableSupport.onDelete(e);
                addOrRemoveChunk(e);// new code
            })
        });
   
   	/* **************************************************************************************************************** */
   	/*			 																										*/
   	/* Override updateOptionalBlock 																					*/
   	/* jenkins/war/src/main/webapp/scripts/hudson-behavior.js															*/
   	/*			 																										*/
   	/* c - checkbox on the optionalBlock																				*/
   	/*																													*/
   	/* The function is being invoked per each optionalBlock when the page is loaded.									*/
   	/* At this point we're not interested in populating the JCL, but reading it from config.xml.						*/
   	/* Thus, we keep a counter that counts the number of times it was invoked,											*/
   	/* and when the counter reaches the total number of optionalBlocks- 												*/
   	/* this is an indication that the function was triggered by the user, and now we can update the JCL.				*/
   	/* When a new build is added the counter is being reset to 0, and the JCL will be populated with the default value. */ 
   	/*																													*/ 
   	/* **************************************************************************************************************** */
   	
   	
   	var origUpdateOptionalBlock = window.updateOptionalBlock;
   	window.updateOptionalBlock = function(c,scroll) {
   	 //	origUpdateOptionalBlock(c,scroll); 	//  original function 	
   	//override the original function because a bug in the scrollIntoView function scrolls to the bottom of the build step
   	 // find the start TR
   		
   	    var s = $(c);
   	    while(!s.hasClassName("optional-block-start"))
   	        s = s.up();

   	    // find the beginning of the rowvg
   	    /*
   	    var vg =s;
   	    while (!vg.hasClassName("rowvg-start"))
   	        vg = vg.next();

   	    var checked = xor(c.checked,Element.hasClassName(c,"negative"));

   	    vg.rowVisibilityGroup.makeInnerVisible(checked);
*/
   	   /*
   	    if(checked && scroll) {
   	        var D = YAHOO.util.Dom;

   	        var r = D.getRegion(s);
   	        r = r.union(D.getRegion(vg.rowVisibilityGroup.end));
   	        scrollIntoView(r);
   	    }
   	    */

   	    if (c.name == 'hudson-tools-InstallSourceProperty') {
   	        // Hack to hide tool home when "Install automatically" is checked.
   	        var homeField = findPreviousFormItem(c, 'home');
   	        if (homeField != null && homeField.value == '') {
   	            var tr = findAncestor(homeField, 'TR')|| findAncestorClass(homeField, 'tr');
   	            if (tr != null) {
   	                tr.style.display = c.checked ? 'none' : '';
   	                layoutUpdateCallback.call();
   	            }
   	        }
   	    }
   		//new code applies only to optionalBlocks that were added by BMC plugin
   		if(c.name.substring(0,3)=="bmc")
   		{
   		    //First, detect if it's a newer version of Jenkins
            if(sectionClassName===undefined)
            {
                el=c;
                while(el.tagName!="DIV")
                    el=el.parentNode;
                limitLoop=0;
                while(el.tagName=="DIV")
                {
                  limitLoop++;
                  el=el.parentNode;
                  if(el.className.includes("jenkins-section"))
                   {
                        sectionClassName="jenkins-section";
                        break;
                   }
                   if (limitLoop==7)
                   break;
                }
            }
            if(sectionClassName=="jenkins-section")
            {
                el=c;

                 while(!el.className.includes("jenkins-form-item"))
                    el=el.parentNode;
                 el=el.parentNode;
                 while(!el.className.includes("jenkins-form-item"))
                    el=el.parentNode;
                 el=el.parentNode;
                 while(!el.className.includes("jenkins-form-item"))
                    el=el.parentNode;
                previous= el.previousSibling.children[1];
            }
            else
            {
                c.parentNode.parentNode.parentNode.children[3].getElementsByTagName("input")[0].checked=c.checked;
                previous=c.parentNode;
                while(previous.tagName=="DIV")
                {
                    previous=previous.parentNode;
                    if(previous.className.includes("repeated-container"))
                        break;
                }
                previous=previous.parentNode.parentNode.previousSibling;
   			}

   			if(previous!=null)
   			{
	   			//id=[notes-stepid]
	   			curStepid=previous.getElementsByTagName("input")[0].id.substring(6);
	   			toggleOptionalOperands(c,curStepid);
   			}
   			updateOptionalBlockCounter++;
   		}
   		
   		
   			
   	}
	
   	/*
	 * Allowing running code onLoad of the page - triggered by "Configure"
	 */	
	// creator=function(){
	// alert("creator");
	// Behaviour.apply();
	// onBuildInit(); }
	// Behaviour.addLoadEvent(creator);
	 	 
       
     // Delay execution to ensure that elements are loaded
     myVar=window.setTimeout(onBuildInit,100);
    
     /* *********************************************************************** */
     /*																			*/
	 /* Initialization function that is being invoked per each BMC build-step	*/
	 /* and hides irrelevant elements per each element chunk.					*/
	 /* !!!No need to populate jcl, it takes the last value in config.xml		*/
     /*																			*/     
	 /* *********************************************************************** */
   	function onBuildInit()
   	{	   	
   		var currentStepBuild;
   		var currentAction;
   		var currentElementType; 
   		
   		// If the last operand hasn't been loaded yet, try again
   		if (document.getElementsByName("msg").length==0 || document.getElementsByClassName("visited").length==0  )   	
   		{
   			window.setTimeout(onBuildInit,50);
   			return;   			
   		}	   		  		
		
   		// Identify the current build step that executes the script and isolate the step id
   		for(singleBuild of document.getElementsByName("builder"))
   		{  				
   			if(singleBuild.getElementsByClassName("visited")[0].value!="visited")
   			{
   				currentStepBuild=singleBuild;
   				// Hide the build step container until all the irrelevant operands are hidden to avoid flashing
   				//currentStepBuild.style.visibility = "hidden";
   				singleBuild.getElementsByClassName("visited")[0].value="visited";
   				
   				// isolate the stepid
   				for(sel of singleBuild.getElementsByTagName("select"))
   	   				if(sel.name=="jclType")
   	   				{
   	   					curStepid=sel.id.substring(8);
   	   					//displayRelevantFields(curStepid,singleBuild);
   	   					break;
   	   				}   				
   				// break the elements container into chunks and display relevant operands per chunk
   				for (currentChunk of currentStepBuild.getElementsByTagName("div"))
   		   			if(currentChunk.getAttribute("name")=="builder.dlistRecords")
   		   			{
   		   				currentAction=undefined;
   		   				currentElementType=undefined;	
   		   				for(sel of currentChunk.getElementsByTagName("select"))
   		   				{   		   					
   		   					if(sel.name=="action")
   		   					{
   		   						// If action is not yet loaded try again
								// [class="select-ajax-pending"]
   		   						if(sel.className.includes("pending"))
   		   						{   		   							
	   		   						singleBuild.getElementsByClassName("visited")[0].value="";
	   	   		   					window.setTimeout(onBuildInit,50);
	   	   		   					return;
   		   						}
   		   						else	
   		   							currentAction=sel.value;
   		   					}
   		   					else if(sel.name=="elementType")
   		   					{
   		   						// If element type is not yet loaded try again
								// [class="select-ajax-pending"]
   		   						if(sel.className.includes("pending"))
		   						{		   							
	   		   						singleBuild.getElementsByClassName("visited")[0].value="";
	   	   		   					window.setTimeout(onBuildInit,50);
	   	   		   					return;
		   						}
   		   						else
   		   							currentElementType=sel.value;
   		   					}	
   		   					if(currentAction!=undefined && currentElementType!=undefined)
   		   						break;
   		   				}
   		   				// If action and element type are not yet loaded try
						// again
   		   				if(currentAction=="" || currentElementType=="")
   		   				{
   		   					singleBuild.getElementsByClassName("visited")[0].value="";
   		   					window.setTimeout(onBuildInit,50);
   		   					return;
   		   				}
   		   				
   		   				// Hide IMS Command
   		   				if(currentAction=="EXECUTE")
   		   					currentChunk.getElementsByClassName('setting-name')[3].parentNode.style.display="block";
   		   				else
   		   				{
   		   				    if(sectionClassName=="jenkins-section")
   		   				        currentChunk.children[3].style.display="none";
   		   				    else
   		   					    currentChunk.getElementsByClassName('setting-name')[3].parentNode.style.display="none";
   		   				}
   		   				displayRelevantOperands(currentAction,currentElementType,currentChunk,curStepid);
   		   			}
   				
   				// initialize the following values for Configure
   				if(document.getElementById("coord-"+stepid).checked==true)
   					checkJclCoord=",TYPE=COORD";
   				if(document.getElementById("mark-"+stepid).checked==true)
   					checkJclMark=",MARK=YES";
   				if(document.getElementById("dependent-"+stepid).checked==true)
   					createJclDepPart="DEPENDENT=YES\n";	
   				if(document.getElementById("vds-"+stepid).value!="")
   					createJclVdsPart="VDS=${VARLIST}\n";
   				
   				//moved it to the end to avoid a situation where dependent element is not loaded yet and throws a null exception
   				displayRelevantFields(curStepid,singleBuild);
   				
   				// Show the step only when all the required operands are
				// hidden/visible to avoid flashing
   				currentStepBuild.style.visibility = "visible";
   			}   			
   		}   		
	}
   	
   
   	/**
   	 * 
   	 * @param container - Load library container
   	 * @returns
   	 */
   	function refreshLoadLibs(container)
   	{  		
   		// Reset load libraries
   		i=0;
  		while (loadLibs.length > 0)
				loadLibs.pop();
  		// Count the number of load libs in a specific container
  		for (x of container.children)  		  				
  			if(x.className.includes("repeated-chunk"))
  			{	
  				if(i==0)		  									
  					loadLibs.push("//STEPLIB   DD DISP=SHR,DSN=${DLP_LOAD0}\n");
  				else
  					loadLibs.push("//          DD DISP=SHR,DSN=${DLP_LOAD"+i+"}\n");	
  					
				i++;		 
			} 	
   	}
   	
   	/*
	 * Because a new script is being activated for each build step, we lose
	 * dlistElement. The function calculates Delta List records on a single
	 * build step
	 */
   	function refreshDlistElements(container,deletedChunk)
   	{
   		// Clear dlistElements
   		dlistElements.splice( 0, dlistElements.length );
   		
   		for(chunk of container.children)
   		{
   			var currentAction=null;   			
   			var currentElementType=null;
   			
   			if(!chunk.className.includes("repeated-chunk"))
   				continue;

   			// Skip the potential chunk for deletion
   			if(chunk==deletedChunk)
   				continue;
   			// When Add Record triggers this function or when the page is first loaded the last element values
			// are not loaded yet
   			/*if(chunk.className.includes("last") && (caller=="AddChunk" || caller==null) )
   			{
   				currentAction="ADD";
   				currentElementType="APPLCTN"
   			}
   			*/
   				
   			// Determine selected action and element type
   			else for(sel of chunk.getElementsByTagName("select"))
			{
				if(sel.name=="action")					
					currentAction=(sel.value=="") ? "ADD" : sel.value; // When Add Record triggers this function or when the page is first loaded the last element values
																	   // are not loaded yet
					
				else if(sel.name=="elementType")
						currentElementType= (sel.value=="") ? "APPLCTN" : sel.value;

				if(currentAction!=undefined && currentElementType!=undefined)
					break;
			}
   			
   		
   			// Create new delta list record
   			dlistElement=new Object();
   			dlistElement.action=currentAction;
   			dlistElement.elType=currentElementType;
   			dlistElement.operands=new Array();  			

   			// Determine the operands associated with the action
   			for(obj of objs)
   			{			
				if(obj.action==currentAction)
				{
					for(child of obj.children)
					{
						if(child.elemenType==currentElementType)
		  				{
		  					for(op of child.operands)
		  					{  		  						
	  		  					for(inp of chunk.getElementsByTagName("input"))
	  								if(inp.name==op.id)
	  								{
				  		  				if(op.optional==true)	
				  		  				{
				  		  					//identify the checkbox related to this input entry
				  		  					if(getOpBlckCheckElFromInput(inp).checked==true)
				  		  						dlistElement.operands.push(op.label);  						
				  		  				}						  						
					  						else if(op.optional==false)  		  							
					  							dlistElement.operands.push(op.label);
	  								}
	  							for(sel of chunk.getElementsByTagName("select"))
	  								if(sel.name==op.id)
	  								{	  									
	  									if(op.optional==true)
				  						{  	
	  										//identify the checkbox related to this select entry
	  										if(getOpBlckCheckElFromInput(sel).checked==true)
	  										{
	  											// if the operand is not allowed
												// with any other operand clear
												// other selections RANDONY
												// RLDAREAS IOVFEXT
	  											if(op.distinct==true)
	  											{	
	  												while (dlistElement.operands.length > 0)
	  													dlistElement.operands.pop();
	  												dlistElement.operands.push(op.label);
	  												break;
	  											}
	  											else
	  												dlistElement.operands.push(op.label);
	  										}
				  						}
				  						else if(op.optional==false)  		  							
				  							dlistElement.operands.push(op.label);
	  								}		  						
		  					}
		  				}		  								  			
					}
					break;
				}  				
			}
   			dlistElements.push(dlistElement);
   		}
   		
   	}

   	/*
   	 * The Jenkins form was changed from a table to div
   	 */
   	function displayRelevantFields(stepid, build)	
   	{	
   		select = document.getElementById("jclType-"+stepid);
		selectedData = select.options[select.selectedIndex].value;   		
		
		if(selectedData=="crtDeltaList")
		{
			document.getElementById('dPds-'+stepid).parentNode.parentNode.style.display = "block";	
			document.getElementById('dlist-'+stepid).parentNode.parentNode.style.display = "block";				 		
	   		document.getElementById('title-'+stepid).parentNode.parentNode.style.display = "block";
	   		document.getElementById('notes-'+stepid).parentNode.parentNode.style.display = "block";		   		
	   		document.getElementById('vds-'+stepid).parentNode.parentNode.style.display = "block";
	   		document.getElementById('dependent-'+stepid).parentNode.parentNode.style.display = "block";	
	   		
			document.getElementById('dOptionsPds-'+stepid).parentNode.parentNode.style.display = "none";
			document.getElementById('target-'+stepid).parentNode.parentNode.style.display = "none";	
			document.getElementById('mode-'+stepid).parentNode.parentNode.parentNode.style.display = "none";
			document.getElementById('mark-'+stepid).parentNode.parentNode.style.display = "none";
			document.getElementById('coord-'+stepid).parentNode.parentNode.style.display = "none";
			
		}
		else if(selectedData=="check")
		{
			document.getElementById('dOptionsPds-'+stepid).parentNode.parentNode.style.display = "block";				
			document.getElementById('dPds-'+stepid).parentNode.parentNode.style.display = "block";	
			document.getElementById('dlist-'+stepid).parentNode.parentNode.style.display = "block";
			document.getElementById('target-'+stepid).parentNode.parentNode.style.display = "block";
			document.getElementById('mode-'+stepid).parentNode.parentNode.parentNode.style.display = "block";
			document.getElementById('mark-'+stepid).parentNode.parentNode.style.display = "block";
			document.getElementById('coord-'+stepid).parentNode.parentNode.style.display = "block";

			document.getElementById('vds-'+stepid).parentNode.parentNode.style.display = "none";
			document.getElementById('title-'+stepid).parentNode.parentNode.style.display = "none";
	   		document.getElementById('notes-'+stepid).parentNode.parentNode.style.display = "none";	   		
	   		document.getElementById('dependent-'+stepid).parentNode.parentNode.style.display = "none";				
			// hide the "Elements to add" repeatable block
	   		build.getElementsByClassName('repeated-container')[1].style.display = "none";	
	   		build.getElementsByClassName('repeated-container')[1].parentNode.parentNode.style.display = "none";	
		}
		
		else if(selectedData=="exec")
		{						
			document.getElementById('dOptionsPds-'+stepid).parentNode.parentNode.style.display = "block";				
			document.getElementById('dPds-'+stepid).parentNode.parentNode.style.display = "block";	
			document.getElementById('dlist-'+stepid).parentNode.parentNode.style.display = "block";
			document.getElementById('target-'+stepid).parentNode.parentNode.style.display = "block";
			document.getElementById('mode-'+stepid).parentNode.parentNode.parentNode.style.display = "block";

			document.getElementById('mark-'+stepid).parentNode.parentNode.style.display = "block";
			document.getElementById('coord-'+stepid).parentNode.parentNode.style.display = "block";

			document.getElementById('vds-'+stepid).parentNode.parentNode.style.display = "none";
			document.getElementById('title-'+stepid).parentNode.parentNode.style.display = "none";
	   		document.getElementById('notes-'+stepid).parentNode.parentNode.style.display = "none";   		
	   		document.getElementById('dependent-'+stepid).parentNode.parentNode.style.display = "none";				
			// hide the "Elements to add" repeatable block
	   		build.getElementsByClassName('repeated-container')[1].style.display = "none";
	   		build.getElementsByClassName('repeated-container')[1].parentNode.parentNode.style.display = "none";	
			
			
		}
   	}
	
	  /**
		 * Display relevant operands according to action and element type in a
		 * specific chunk in a specific container
		 * 
		 * @param action
		 * @param elementType
		 * @param chunk -
		 *            represents a single record in a Delta list
		 * @param stepid -
		 *            identifies the relevant build step
		 * @returns
		 */  
	
	function displayRelevantOperands(action, elementType, chunk, stepid)	{		
		/*
		if(action=="RELOAD")
		{
			// hide the "Operands" section header
			if( elementType=="APPLCTN")				
				chunk.getElementsByClassName("section-header")[0].style.display="none";
			else
				chunk.getElementsByClassName("section-header")[0].style.display="block";
		}
		*/
		var i=Array.from(chunk.parentNode.children).indexOf(chunk);		
			
  			for(obj of objs)
  			{
  			/*
  				if(action=="EXECUTE")
  				{
  					// hide all operands
  					for(child of obj.children)
  						for(op of child.operands)
  						{
  							// document.getElementsByName(op.id)[indx].parentNode.parentNode.style.display
							// = "none";
  							for(inp of chunk.getElementsByTagName("input"))
  								if(inp.name==op.id)
  									inp.parentNode.parentNode.style.display = "none";
  							for(sel of chunk.getElementsByTagName("select"))
  								if(sel.name==op.id)
  									sel.parentNode.parentNode.style.display = "none";
  						}
  					//hide element type/name
  					for( var c of chunk.getElementsByClassName("setting-name") )	
  					{
  						if(c.innerHTML=="Element name" || c.innerHTML=="Element type" )	   	
  				   			c.parentNode.style.display = "none";
  				   		else if(c.innerHTML=="IMS COMMAND")	
  				   			c.parentNode.style.display = "block";
  				    }
  					// hide the "Operands" section header
  					chunk.getElementsByClassName("section-header")[0].style.display="none";
  					
  				}
  				*/
  				if(obj.action==action)
  				{
  					
  					for(child of obj.children)
  					{
  						if(child.elemenType==elementType)
  		  				{
  							for(inp of chunk.getElementsByTagName("input"))  								
  		  					{   
  								match=false;
  								if(inp.name=="ims_cmd" || inp.name=="elementName" || inp.type=="checkbox")
  									continue;
  								
  								optBlock=inp.parentNode;
	  							while(optBlock.tagName=="DIV")
									{
										optBlock=optBlock.parentNode;
										if(optBlock.className.includes("optionalBlock-container"))
											break;
									}
	  							for(formContainer of optBlock.children)
	  								if(formContainer.className.includes("form-container"))
	  									break;
  								
  								for(op of child.operands)
	  								if(inp.name==op.id)
	  								{	  									
	  									//inp.parentNode.parentNode.parentNode.parentNode.style.display = "block";
	  									optBlock.style.display = "block"
	  										
	  									// Display checkbox for optional operand
	  									if(op.optional==true)
	  									{
	  										//inp.parentNode.parentNode.parentNode.parentNode.firstChild.firstChild.style.display="initial";
	  										//optBlock.firstChild.firstChild.firstChild.style.display="initial";
	  										isolateCboxFromSpan(optBlock).disabled=false;
	  										//isolateCboxFromSpan(optBlock).checked=false;
	  									}
				  		  				
	  									// Hide checkbox for required operand and display the expandable hidden part
				  		  				else if(op.optional==false)
				  		  				{
				  		  				    if(sectionClassName=="jenkins-section")
				  		  				    {
                                                isolateCboxFromSpan(optBlock).disabled=true;
                                                isolateCboxFromSpan(optBlock).checked=false;
                                             }
                                            else
					  		  				    optBlock.firstChild.firstChild.firstChild.style.display="none";
				  		  					formContainer.style.display="block";
				  		  					if(sectionClassName="jenkins-section")
				  		  					{
				  		  					    formContainer.className="form-container tr"; //remove form-container--hidden
				  		  					    formContainer.nextSibling.className="form-container tr";
				  		  					    formContainer.nextSibling.style.display="block";
				  		  					}

					  		  				//getOpBlckCheckElFromInput(inp).style.display="none";
				  		  					//inp.parentNode.parentNode.parentNode.style.display="block";
				  		  					//inp.parentNode.parentNode.parentNode.previousSibling.style.display="block";
				  		  				}
				  		  				match=true;
				  		  				break;
	  								}
  								if(match==false)
  								{
  									inp.parentNode.parentNode.parentNode.parentNode.style.display = "none";
  									//inp.parentNode.parentNode.previousSibling = "none;"
  								}
  		  					}
  							
	  						for(sel of chunk.getElementsByTagName("select"))
	  						{	  							
	  							match=false;
	  							if(sel.name=="elementType" || sel.name=="action")
  									continue;
	  							
	  							optBlock=sel.parentNode;
	  							while(optBlock.tagName=="DIV")
									{
										optBlock=optBlock.parentNode;
										if(optBlock.className.includes("optionalBlock-container"))
											break;
									}
	  							for(formContainer of optBlock.children)
	  								if(formContainer.className.includes("form-container"))
	  									break;
	  							
	  							for(op of child.operands)
	  								if(sel.name==op.id)
	  								{	  									
	  									optBlock.style.display = "block"; //display the whole select entry including help+validation areas
	  									
	  									// Display checkbox for optional operand
	  									if(op.optional==true)
	  									{
	  										//optBlock.firstChild.firstChild.firstChild.style.display="initial";
	  										isolateCboxFromSpan(optBlock).disabled=false;
	  										//isolateCboxFromSpan(optBlock).checked=false;
	  									}
	  									
	  									// Hide checkbox for required operand and display the expandable hidden part
				  		  				else if(op.optional==false)
				  		  				{
				  		  					if(sectionClassName=="jenkins-section")
				  		  					{
				  		  					    isolateCboxFromSpan(optBlock).disabled=true;
				  		  					    isolateCboxFromSpan(optBlock).checked=false;
				  		  					    }
				  		  					else
				  		  					    optBlock.firstChild.firstChild.firstChild.style.display="none";


				  		  					
				  		  					formContainer.style.display="block";
				  		  					if(sectionClassName=="jenkins-section")
				  		  					{
				  		  					    formContainer.className="form-container tr"; //remove form-container--hidden
				  		  					    formContainer.nextSibling.className="form-container tr";
				  		  					    formContainer.nextSibling.style.display="block";
				  		  					}
				  		  					//optBlock.children[2].style.display="block"; //empty div
				  		  					
				  		  				}
	  									
	  									match=true;
	  									break;
	  								}
	  							if(match==false)
	  							{
	  								optBlock.style.display = "none"; //hide the select entry
  									//sel.parentNode.parentNode.previousSibling = "none;" //hide the previousSibling checkbox entry
	  							}
  		  					}
  		  				}
  		  				else  		  				
  		  					for(op of child.operands)
  		  					{
  		  						for(inp of chunk.getElementsByTagName("input"))
  		  						{
	  		  						optBlock=inp.parentNode;
		  							while(optBlock.tagName=="DIV")
										{
											optBlock=optBlock.parentNode;
											if(optBlock.className.includes("optionalBlock-container"))
												break;
										}
  		  							if(inp.name==op.id)
  		  								optBlock.style.display = "none";
  		  						}
  		  						for(sel of chunk.getElementsByTagName("select"))
  		  						{
	  		  						optBlock=sel.parentNode;
		  							while(optBlock.tagName=="DIV")
										{
											optBlock=optBlock.parentNode;
											if(optBlock.className.includes("optionalBlock-container"))
												break;
										}
		  							if(sel.name==op.id)
		  								optBlock.style.display = "none";
  		  						}
  		  					}
  		  								  			
  					}
  					break;
  				}  				
  			}	  			
		}
	function isolateCboxFromSpan(optBlck)
       	{
       	    xmlString=optBlck.firstChild.firstChild.firstChild.innerHTML;
            tmpArr=xmlString.split("id=\"");
            arr2=tmpArr[1].split("\"");
            id=arr2[0];
            //cbox = (new DOMParser().parseFromString(xmlString, "text/xml")).firstChild;
            return cbox = document.getElementById(id);
       	}
	/*
	 * Function that is being triggered by Add/Delete Record/Library
	 */
		function addOrRemoveChunk(event)
  		{
  		    if(event.target===undefined)
  		    {
                if(event.parentNode.innerHTML.includes("Delete library"))
                        {
                            var previous=event.parentNode;

                            while(previous.tagName!="DIV")
                                previous=previous.parentNode;

                            while(previous.tagName=="DIV")
                            {
                                previous=previous.parentNode;
                                if(previous.className=="repeated-container")
                                    libsContainer=previous;

                            }
                            // id="dpds-#"
                            curr_stepid=libsContainer.parentNode.parentNode.nextSibling.getElementsByTagName("input")[0].id.substring(5);

                            refreshLoadLibs(libsContainer);
                            loadLibs.pop();

                            populateJcl(curr_stepid,null);
                        }

                else if(event.parentNode.innerHTML.includes("Delete record"))
                        {
                            var container;
                            previous=event.parentNode;

                            while(!previous.className.includes("repeated-chunk"))
                            {
                                previous=previous.parentNode;
                                while(previous.className==null)
                                    previous=previous.parentNode;
                            }
                            chunk=previous;
                            container=chunk.parentNode;

                            // id="notes-#"
                            curr_stepid=container.parentNode.parentNode.previousSibling.getElementsByTagName("input")[0].id.substring(6);

                            // var i=Array.from(container.children).indexOf(chunk);
                            // dlistElements.splice(i,1);

                            refreshDlistElements(container, chunk);

                            populateJcl(curr_stepid,null);
                        }// end of Delete Record
            }
	  		else if(event.target.innerHTML=="Add record")
	  		{	
	  			var previous=event.target;  //button
	  			var curChunk;
	  			while(previous.tagName!="DIV")
	  			{
	 	 			previous=previous.parentNode;
	 	 			
	 	 			// hide IMS Command
	 	 			if(previous.className=="repeated-container")
	 	 			{
	 	 			    for(x of previous.children)
	 	 			    {
	 	 			    	if(x.className.includes("repeated-chunk last") || x.className.includes("repeated-chunk first last"))
	 	 			    	{	
	 	 						//x.getElementsByClassName('setting-name')[3].parentNode.style.display="none";
	 	 						x.children[4].style.display="none";
	 	 						curChunk=x;
	 	 			    	}
						}	
					}
		  		}
		  		// id="notes-#"
		  		curr_stepid=previous.parentNode.parentNode.previousSibling.getElementsByTagName("input")[0].id.substring(6);
		  		
		  		displayRelevantOperands("ADD","APPLCTN",curChunk,curr_stepid);
		  		
		  		// Identify the build step chunk
		  		temp=curChunk.parentNode;// Elements container
		  		while(!temp.className.includes("repeated-chunk"))
		  			temp=temp.parentNode;		  		
		  		refreshLoadLibs(temp.getElementsByClassName("repeated-container")[0]);
		  		
		  		refreshDlistElements(curChunk.parentNode, null);
		  		populateJcl(curr_stepid,null);
		  	}			
	  	 		 			 		  			 	
    			
    		else if(event.target.innerHTML=="Add library")
    		{    			
    			var previous=event.target;
    			
	  			while(previous.tagName!="DIV")
	  			{
	  				previous=previous.parentNode;
	  				if(previous.className=="repeated-container")
	  					libsContainer=previous;	  				
	  			}
	  			
	  			// id="dpds-#"	  			
	  			curr_stepid=libsContainer.parentNode.parentNode.nextSibling.getElementsByTagName("input")[0].id.substring(5);
	  			
	  			refreshLoadLibs(libsContainer);	
	  			
		  		//find the elements container
		  		div=libsContainer.parentNode.parentNode.nextSibling;
		  		while(div.getElementsByClassName("repeated-container").length==0)
		  			div=div.nextSibling;
		  		refreshDlistElements(div.getElementsByClassName("repeated-container")[0],null);
	  			
		  		populateJcl(curr_stepid,null);
    		}// end of Add library
    			
    		

    		
 		}// end of function
	 
	  	/*
	  	 * 	populateJcl is invoked before onBuildInit, maybe while reading the config.xml
	  	 *  When starting fresh, the dlistElements contains: {action: "", elType: "", operands: Array(0)}
	  	 */	  	
		function populateJcl(stepid,event)
		{
			
    		select = document.getElementById("jclType-"+stepid);
			selectedData = select.options[select.selectedIndex].value;
	
			// identify the relevant build step that matches the current stepid
			curerntBuildStep=select;
			
			while(!curerntBuildStep.className.includes("repeated-chunk"))							
				curerntBuildStep=curerntBuildStep.parentNode;		
						
			displayRelevantFields(stepid,curerntBuildStep);
			
			// if the function was triggered by onchange then hide any visible help Divs
			if(event!=null)
		   	{
		   		// Hide any visible help DIVs
				for (var hlp of curerntBuildStep.getElementsByClassName("help"))
					hlp.style.display="none";
		   	}
			
			if(selectedData=="crtDeltaList")
			{	
			   	var rpt=curerntBuildStep.getElementsByClassName('repeated-container');
			   	for(var r of rpt)
			   	{
			   		r.parentNode.parentNode.style.display = "block";
			   		r.style.display = "block";
			   	}
			   	
			   	// if the function was triggered by onchange then remove all
				// delta list records and leave only one
			   	if(event!=null)
			   	{	   		
			   		
					i=0;
			   	
				   	for(var chunk of  curerntBuildStep.getElementsByClassName('repeated-container')[1].getElementsByClassName('repeated-chunk'))
				   	{
				   		if(i!=0)
				   			// simulate click on delete button
				   			chunk.getElementsByTagName('BUTTON')[0].click();
				   		i++;
				   	}
			   	}
						
				createJclCntlPart="";	
				createJclLoadlibsPart="";	
				createJclOptOperands="";
				
				i=0;
				
				for(singleEl of dlistElements)
				{
						var operands="\n";
						opArray=new Array();
						
						for(op of singleEl.operands)
						{
							if(operands.length + (op+"=${"+op+i+"} ").length<=80)
							{
								operands+=op+"=${"+op+i+"} ";
								
							}
							else
							{
								opArray.push(operands+"\n");								
								operands=op+"=${"+op+i+"} ";
								
							}
							//last operand
							if(singleEl.operands.indexOf(op)==singleEl.operands.length-1)
								opArray.push(operands);	
						}
						
						//no operands
						if(singleEl.operands.length==0)
							operands="";
						
						
						if(singleEl.action=="ADD" || singleEl.action=="REVISE" || singleEl.action=="ADDREV" || singleEl.action=="DELETE")
							createJclCntlPart+="\n${ACTION"+i+"} ${ELEMENT_TYPE"+i+"} ${ELEMENT_NAME"+i+"}"+opArray;
												
						
						else if(singleEl.action=="EXECUTE")						
							createJclCntlPart+="\nEXECUTE ${IMS_COMMAND"+i+"}";
						
						else if(singleEl.action=="RELOAD")
						{
							if(singleEl.elType=="APPLCTN")
								createJclCntlPart+="\n${ACTION"+i +"} ${ELEMENT_NAME"+i+"} ACB"+opArray;
								
							else if(singleEl.elType=="DATABASE")
								createJclCntlPart+="\n${ACTION"+i +"} ${ELEMENT_NAME"+i+"} DMB"+opArray;
						}
						i++;	
						createJclCntlPart=createJclCntlPart.replace(/,/g,"");
				}
				
		     	for(var loadLib of loadLibs)
		     		createJclLoadlibsPart+=loadLib ;		
		     	
		     	if(createJclVdsPart.trim()!="")
					createJclVdsPart="VDS=${VARLIST}\n";		 
		     	document.getElementById('jclContent-'+stepid).value=createJclJobcardPart+createJclLoadlibsPart+createJclBodyPart+createJclDepPart+createJclVdsPart+createJclNotesPart+createJclCntlPart+createJclEndPart;     
					
			}// end of crtDeltaList
			
			else if(selectedData=="check" || selectedData=="exec" )
			{	
				var action;
				if(selectedData=="check")
					action=" CHECK";
				else if(selectedData=="exec")
					action=" EXEC";
				
				createJclLoadlibsPart="";
				
				// if the function was triggered by onchange then 
			   	if(event!=null)			
			   	{
			   	//if(loadLibs.length==0)
					refreshLoadLibs(curerntBuildStep.getElementsByClassName('repeated-container')[0]);
			   	}
				
		
				for(var loadLib of loadLibs)
		     		createJclLoadlibsPart+=loadLib ;	
				
				if(createJclVdsPart.trim()!="")
					createJclVdsPart=",V=${VARLIST}";
				
				document.getElementById('jclContent-'+stepid).value=
				    "${JOB_CARD}\n" +
				   	"//DLPBTSCT EXEC PGM=DLPBTSCT,PARM='**BATCH',REGION=4M\n"+
					"//STEPLIB   DD DISP=SHR,DSN=${DLP_OPTIONS_PDS}\n" +           
					 createJclLoadlibsPart.replace(/STEPLIB/g,'       ')+        
					"//DELTAPDS  DD DISP=SHR,DSN=${DELTA_PDS}\n" +           
					"//SYSPRINT  DD SYSOUT=*\n" +				        
					"//SYSIN     DD *\n" +           
	 				action+" ${DELTA_LIST_NAME},TARGET=${TGT},MODE=${MOD}" +
	 				checkJclMark+checkJclCoord+createJclVdsPart+"\n"+
					"/*\n" +           
					"//\n";	 					
			}// end of check/exec
			
			
		}// end of function
		
		function elementTypeChanged(obj,stepid)
		
		{
			
			selectedData = obj.options[obj.selectedIndex].value;
			selectedElementType=selectedData;			
			// <td> <tr> <tbody>
			if(sectionClassName="jenkins-section")
			    selectedAction = obj.parentNode.parentNode.parentNode.previousSibling.getElementsByTagName("select")[0].value;
			else
			    selectedAction = obj.parentNode.parentNode.parentNode.getElementsByTagName("select")[0].value;
			
			var i=Array.from(document.getElementsByName("elementType")).indexOf(obj);
						
				
			while(!obj.className.includes('repeated-chunk'))				     					     				
		    	obj=obj.parentNode;	
			
			
			for(option of document.getElementsByName("action")[i].children)
			{					
				if(option.value=="RELOAD")
				{
					if(selectedElementType=="TRANSACT" || selectedElementType=="RTCODE" || selectedElementType=="TERMINAL" || selectedElementType=="LTERM" || selectedElementType=="SUBPOOL" )
						option.style.display="none";
					else
						option.style.display="block";
					break;
				}
				
			}
			
			// hide the "Operands" section header
			if(selectedAction=="RELOAD" && selectedElementType=="APPLCTN")
			{
			    if(sectionClassName="jenkins-section")
			        obj.getElementsByClassName("jenkins-section")[0].style.display="none";
			    else
				    obj.getElementsByClassName("section-header")[0].style.display="none";
			}
			else
			{
			    if(sectionClassName="jenkins-section")
			        obj.getElementsByClassName("jenkins-section")[0].style.display="block";
			    else
				    obj.getElementsByClassName("section-header")[0].style.display="block";
			}
			// Hide any visible help DIVs
			for (var hlp of obj.getElementsByClassName("help"))
				hlp.style.display="none";
			
			displayRelevantOperands(selectedAction,selectedElementType,obj,stepid);
			
			// Identify the build step chunk
	  		temp=obj.parentNode;// Elements container
	  		while(!temp.className.includes("repeated-chunk"))
	  			temp=temp.parentNode;		  		
	  		refreshLoadLibs(temp.getElementsByClassName("repeated-container")[0]);
	  		
			
			refreshDlistElements(obj.parentNode,null);
			populateJcl(stepid,null);
		}
		/** ************************************************************************************************ */
		/* actionChanged */
		/** ************************************************************************************************ */
		function actionChanged(obj,stepid)
		{
			
			selectedData = obj.options[obj.selectedIndex].value;
			selectedAction=selectedData;
			
			var current=obj;
			while(current.tagName!="DIV")
			    current=current.parentNode;
			
			while(!current.className.includes('repeated-chunk'))				     					     				
		    	current=current.parentNode;

		    selectedElementType= current.getElementsByTagName("select")[1].value;
			
			// Hide any visible help DIVs
			for (var hlp of current.getElementsByClassName("help"))
				hlp.style.display="none";
			
			var i=Array.from(current.parentNode.children).indexOf(current);


			if(selectedData=="EXECUTE")
			{
				//display the RELOAD value in the Action select box, incase it was previously hidden
				/*
				for(option of document.getElementsByName("action")[i].children)
				{					
					if(option.value=="RELOAD")
					{					
						option.style.display="block";
						break;
					}					
				}				
				*/
				for( var c of current.getElementsByClassName("jenkins-form-label help-sibling") )
				{
					if(c.innerText.includes("Element name") || c.innerText.includes("Element type") || c.innerText.includes("Delta list operands"))
			   			c.parentNode.style.display = "none";

			   		else if(c.innerText.includes("IMS COMMAND"))
			   			c.parentNode.style.display = "block";
			    }
				// hide the "Operands" section header
				operandsSection=current.getElementsByClassName("jenkins-section")[0];
				/*
				while(!operandsSection.className.includes("jenkins-form-item tr"))
					operandsSection=operandsSection.parentNode;
					*/
				operandsSection.style.display="none";
			}
			
			else if(selectedData=="ADD" || selectedData=="ADDREV" || selectedData=="REVISE" || selectedData=="DELETE" )
			{

					// display the "Operands" section header
					operandsSection=current.getElementsByClassName("jenkins-section")[0];
				/*
					while(!operandsSection.className.includes("jenkins-form-item tr"))
						operandsSection=operandsSection.parentNode;
				*/
					if(selectedData=="DELETE")
					    operandsSection.style.display="none";
					else
					    operandsSection.style.display="block";

				/*for( var c of current.getElementsByClassName("setting-name") )
				{
					if(c.innerText=="Element name" || c.innerText=="Element type" || c.innerText=="Delta list operands")	   	
			   			c.parentNode.style.display = "block";
			   		else if(c.innerText=="IMS COMMAND")	
			   			c.parentNode.style.display = "none";
			   }*/
			   for( var c of current.getElementsByClassName("jenkins-form-label help-sibling") )
               				{
               					if(c.innerText.includes("Element name") || c.innerText.includes("Element type") || c.innerText.includes("Delta list operands"))
               			   			c.parentNode.style.display = "block";

               			   		else if(c.innerText.includes("IMS COMMAND"))
               			   			c.parentNode.style.display = "none";
               			    }
				
				for(option of document.getElementsByName("elementType")[i].children)
				{					
					if(option.value!="APPLCTN" && option.value!="DATABASE" )
						option.style.display="block";
				}
				displayRelevantOperands(selectedAction,selectedElementType,current,stepid);	
			}
			
			else if(selectedData=="RELOAD")
			{
				if(selectedElementType!="APPLCTN")
					current.getElementsByClassName("jenkins-section")[0].style.display="block";
				else
					current.getElementsByClassName("jenkins-section")[0].style.display="none";

				for( var c of current.getElementsByClassName("jenkins-form-label") )
				{
					if( c.innerHTML.includes("IMS COMMAND") )
			   			c.parentNode.style.display = "none";
			   		else if(c.innerHTML=="Element name" ||  c.innerHTML=="Element type" )	
			   			c.parentNode.style.display = "block";
			   }
				// reset the default value
				// document.getElementsByName("elementType")[i].value="APPLCTN";
				for(option of document.getElementsByName("elementType")[i].children)
				{					
					if(option.value!="APPLCTN" && option.value!="DATABASE" )
						option.style.display="none";
				}

				document.getElementsByName("elementName")[i].parentNode.parentNode.style.display="block";
				
				displayRelevantOperands(selectedAction,selectedElementType,current,stepid);	
			}
			
			
			
			
			// Identify the build step chunk
	  		temp=current.parentNode;// Elements container
	  		while(!temp.className.includes("repeated-chunk"))
	  			temp=temp.parentNode;		  		
	  		refreshLoadLibs(temp.getElementsByClassName("repeated-container")[0]);
	  			  		
			refreshDlistElements(current.parentNode,null);
			populateJcl(stepid,null);	
			
		}// end of func

		function findParentJenkinsSection(obj)
		{
		    while(obj.tagName!="DIV")
		        obj=obj.parentNode;
		    while(!obj.className.includes("jenkins-section"))
		        obj=obj.parentNode;
		    return obj;
		}
		function toggleDependent(obj,stepid)
		{
			if(obj.checked==true)			
				createJclDepPart="DEPENDENT=YES\n";			
			else			
				createJclDepPart="";

			temp=findParentJenkinsSection(obj);
			refreshLoadLibs(temp.children[2].getElementsByClassName("repeated-container")[0]);
			refreshDlistElements(temp.children[9].getElementsByClassName("repeated-container")[0],null);
			populateJcl(stepid,null);
		}// end of func
		
		function toggleMark(obj,stepid)
		{
			if(obj.checked==true)			
				checkJclMark=",MARK=YES";			
			else			
				checkJclMark="";
			temp=findParentJenkinsSection(obj);
           	refreshLoadLibs(temp.children[2].getElementsByClassName("repeated-container")[0]);
           	refreshDlistElements(temp.children[9].getElementsByClassName("repeated-container")[0],null);
           	populateJcl(stepid,null);


		}// end of func
		
		
		
		function toggleCoord(obj,stepid)
		{
			if(obj.checked==true)			
				checkJclCoord=",TYPE=COORD";			
			else			
				checkJclCoord="";
			temp=findParentJenkinsSection(obj);
            refreshLoadLibs(temp.children[2].getElementsByClassName("repeated-container")[0]);
            refreshDlistElements(temp.children[9].getElementsByClassName("repeated-container")[0],null);
            populateJcl(stepid,null);

		}// end of func
		
		function toggleOptionalOperands(obj,stepid)
		{	
			var currentChunk;
			
			if(obj.tagName=="SELECT")
				currentChunk=getOpBlckCheckElFromInput(obj);
			else if(obj.tagName=="INPUT")	
				currentChunk=obj;
			
			while(!currentChunk.className.includes('repeated-chunk'))				     					     				
				currentChunk=currentChunk.parentNode;				
			
			// Identify the Load library container
	  		temp=currentChunk.parentNode;// Elements container
	  		while(!temp.className.includes("repeated-chunk"))
	  			temp=temp.parentNode;		  		
	  		
	  		var action=null;
	  		var elementType=null;
	  		for (c of currentChunk.getElementsByTagName("select"))
	  		{	
	  			if(c.getAttribute("name")=="action")	  				
	  				action=c.value;	  			
	  			else if(c.getAttribute("name")=="elementType")
	  				elementType=c.value;
	  			if(action!=null && elementType!=null)
	  				break;
	  		}
	  		
	  		//if a checkbox triggered the function	
	  	/*
	  		if(obj.tagName=="INPUT" && obj.checked==true)
	  		{
	  			if(checkMutuallyExclusive(obj,action,elementType,currentChunk))
		  			{	  				
		  				obj.checked=false;
	  					return;
		  			}
		  			checkDependency(obj,action,elementType,c);
	  		}

	  		//if a select onchange triggered the function
	  		else if(obj.tagName=="SELECT")
	  		{	  			
	  			if(checkMutuallyExclusive(getOptBlckCheckbox(getOptionalBlockContainer(obj)),action,elementType,currentChunk))
	  			{	
	  				// keep old value
	  				obj.value=obj.oldvalue;
  					return;
	  			}
	  			checkDependency(getOptBlckCheckbox(getOptionalBlockContainer(obj)),action,elementType,currentChunk);
	  		}		  		
	  */
	  		//When a new item/build step is being added/configured no need to populate the JCL, just read it from the config.xml
	  		if(updateOptionalBlockCounter>=totalNumOfoperands)
	  		{
	  			refreshLoadLibs(temp.getElementsByClassName("repeated-container")[0]);			
	  			refreshDlistElements(currentChunk.parentNode,null);			
	  			populateJcl(stepid,null);
	  		}
		}
		/* ********************************************************************************************** */
		function getOptionalBlockContainer(foldedElement)
		{
			while(foldedElement.tagName!="DIV")
				foldedElement=foldedElement.parentNode;
			while(foldedElement.tagName=="DIV")
			{
				foldedElement=foldedElement.parentNode;
				if(foldedElement.className.includes("optionalBlock-container"))
					break;
			}
			return foldedElement;
		}
		
		/* ********************************************************************************************** */
		
		function getOptBlckCheckbox(optBlckContainer)
		{
			return optBlckContainer.firstChild.firstChild.firstChild;
		}
		
		/* ********************************************************************************************** */
		function getOptBlckSelect(optBlckContainer)
		{
			for(div of optBlckContainer.children)
				if(div.className.includes("form-container"))
					return div.children[1].children[0].children[0];				
			
		}
		/* ********************************************************************************************** */
		/* Return false - if no dependencies ; true - if there are any operands' dependencies			  */	
		/* selectedOperand = operand checkbox														 	  */
		/* ********************************************************************************************** */
		function checkMutuallyExclusive(selectedOperand,action,elementType,currentChunk)
		{
			for(obj of objs)  			
  				if(obj.action==action)
  				{	
  					for(child of obj.children)  					
  						if(child.elemenType==elementType)
  						{
  							for(op of child.operands)
	  							if(selectedOperand.nextSibling.innerHTML==op.label)
	  							{
	  								
	  								if(op.mut_exc!=null)
	  								{	
		  								for(mut of op.mut_exc)
		  								{	
		  									for (mutOperand of currentChunk.getElementsByTagName("select"))
		  									{
		  										if(mutOperand.getAttribute("name")==mut.id)
		  										{                                                                                                  
		  											// if the mutually exclusive operand is a required operand then the checkbox is hidden
		  											if(getOptBlckCheckbox(getOptionalBlockContainer(mutOperand)).style.display=="none")
		  											{			  											
			  											if(mutOperand.value==mut.value && getOptBlckSelect(getOptionalBlockContainer(selectedOperand)).value==mut.matchValue)
			  											{	
			  												alert(op.label+"="+mut.matchValue+ " and "+mut.label+"="+mut.value+" are mutually exclusive");			  												
			  												return true;
			  											}
		  											}
		  											// else if the mutually exclusive operand is an optional operand then the
													// checkbox is displayed
		  											else
		  											{
		  												// if the function was triggered by SELECT onchange then check
														// whether the checkbox is hidden
		  												if(selectedOperand.style.display=="none")
		  												{	
	
				  											if(getOptBlckCheckbox(getOptionalBlockContainer(mutOperand)).checked==true && mutOperand.value==mut.value && getOptBlckSelect(getOptionalBlockContainer(selectedOperand)).value==mut.matchValue)
				  											{	
				  												alert(op.label+"="+mut.matchValue+  " and "+mut.label+"="+mut.value+" are mutually exclusive");		  															  												
				  												return true;
				  											}
		  												}		  												
		  												else
		  												{			  												
				  											if(selectedOperand.checked==true && getOptBlckCheckbox(getOptionalBlockContainer(mutOperand)).checked==true && mutOperand.value==mut.value && getOptBlckSelect(getOptionalBlockContainer(selectedOperand)).value==mut.matchValue)
				  											{	
				  												alert(op.label+"="+mut.matchValue+  " and "+mut.label+"="+mut.value+" are mutually exclusive");		  															  												
				  												return true;
				  											}
		  												}
		  											}
		  											break;
		  										}	  										
		  									}
		  								}
	  								}
	  								break;
	  							}
  							break;
  						}
  					break;
  				}
			return false;
		}
		
		/* ********************************************************************************************** */
		
		function checkDependency(selectedOperand,action,elementType,currentChunk)
		{
			for(obj of objs)  			
  				if(obj.action==action)
  				{	
  					for(child of obj.children)  					
  						if(child.elemenType==elementType)
  						{
  							for(op of child.operands)
	  							if(selectedOperand.nextSibling.innerHTML==op.label)
	  							{
	  								if(op.dependencies!=null)
	  								{	
		  								for(dep of op.dependencies)
		  								{	
		  									for (dependentOperand of currentChunk.getElementsByTagName("select"))
		  									{
		  										if(dependentOperand.getAttribute("name")==dep.id)
		  										{	
		  											// if the dependent operand is a required operand then the checkbox is hidden
		  											if(getOptBlckCheckbox(getOptionalBlockContainer(dependentOperand)).style.display=="none")
		  											{
		  												if (dep.value==undefined && dep.matchValue==undefined)		  												
		  													alert(op.label+ " is only valid if "+dep.label + " is specified" ); 												
		  												
		  												else if(dep.value!=undefined && dep.matchValue==undefined)
		  													if(dependentOperand.value!=dep.value)
		  														alert(op.label+ " is only valid if "+dep.label + "="+dep.value+" is specified");													
		  												
		  												else if(dep.value==undefined && dep.matchValue!=undefined)		  												
		  													if(getOptBlckSelect(getOptionalBlockContainer(selectedOperand)).value==dep.matchValue)		  														
		  														alert(op.label+ "="+dep.matchValue+" is only valid if "+dep.label + " is specified" );
		  												
		  												else if(dep.value!=undefined && dep.matchValue!=undefined)		  												
		  													if(getOptBlckSelect(getOptionalBlockContainer(selectedOperand)).value==dep.matchValue && dependentOperand.value!=dep.value)		  													
		  														alert(op.label+ "="+dep.matchValue+" is only valid if "+dep.label + "="+dep.value+" is specified" );	  												
		  												
		  											}
		  											// else if the dependent operand is an optional operand then the checkbox is displayed
		  											else
		  											{
		  												if (dep.value==undefined && dep.matchValue==undefined && getOptBlckCheckbox(getOptionalBlockContainer(dependentOperand)).checked!=true)		  												
		  													alert(op.label+ " is only valid if "+dep.label + " is specified" ); 												
		  												
		  												else if(dep.value!=undefined && dep.matchValue==undefined)
		  												{	
		  													if(dependentOperand.value!=dep.value || getOptBlckCheckbox(getOptionalBlockContainer(dependentOperand)).checked!=true)
		  														alert(op.label+ " is only valid if "+dep.label + "="+dep.value+" is specified");													
		  												}
		  												else if(dep.value==undefined && dep.matchValue!=undefined)
		  												{	
		  													if(getOptBlckSelect(getOptionalBlockContainer(selectedOperand)).value==dep.matchValue && getOptBlckCheckbox(getOptionalBlockContainer(dependentOperand)).checked!=true)		  														
		  														alert(op.label+ "="+dep.matchValue+" is only valid if "+dep.label + " is specified" );
		  												}
		  												else if(dep.value!=undefined && dep.matchValue!=undefined)
		  												{	
		  													if(getOptBlckSelect(getOptionalBlockContainer(selectedOperand)).value==dep.matchValue && (dependentOperand.value!=dep.value || getOptBlckCheckbox(getOptionalBlockContainer(dependentOperand)).checked!=true))		  													
		  														alert(op.label+ "="+dep.matchValue+" is only valid if "+dep.label + "="+dep.value+" is specified" );
		  												}	
		  											}
		  											
		  											// the dependent operand was
													// identified no need to
													// continue searching
		  											break;
		  										}	  										
		  									}
		  								}
	  								}
	  								
	  								break;
	  							}
  							break;
  						}
  					break;
  				}
			
		}					
		
		
		function handleVds(obj,stepid)
		{
			if(!obj.value.length == 0)
				createJclVdsPart="VDS=${VARLIST}\n";
			else
				createJclVdsPart="";
			
			// Identify the build step chunk
	  		temp=(obj.parentNode.parentNode.parentNode.getElementsByClassName("repeated-container")[1]);// Elements
																										// container
	  		while(!temp.className.includes("repeated-chunk"))
	  			temp=temp.parentNode;		  		
	  		refreshLoadLibs(temp.getElementsByClassName("repeated-container")[0]);
			
	  		
			// <td> <tr> <tbody>
			refreshDlistElements(obj.parentNode.parentNode.parentNode.getElementsByClassName("repeated-container")[1],null);
			populateJcl(stepid,null);
		}
		
		
		function getOpBlckCheckElFromInput(foldedElement)
		{
			//return foldedElement.parentNode.parentNode.parentNode.parentNode.firstChild.firstChild.firstChild;
			while(!foldedElement.className.includes("optionalBlock-container"))
           		    foldedElement=foldedElement.parentNode;
           return isolateCboxFromSpan(foldedElement);
		}
