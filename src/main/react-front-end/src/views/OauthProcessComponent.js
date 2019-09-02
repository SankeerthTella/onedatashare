import React, { Component, Text } from 'react';

import PropTypes from 'prop-types';

import  { Redirect } from 'react-router-dom';
import {store} from '../App';
import {transferPageUrl, oauthPreUrl,
		DROPBOX_TYPE, GOOGLEDRIVE_TYPE, FTP_TYPE, SFTP_TYPE, GRIDFTP_TYPE, HTTP_TYPE,
		sideLeft, sideRight, DROPBOX_NAME, GOOGLEDRIVE_NAME, GRIDFTP_NAME
} from "../constants";
import {eventEmitter} from "../App";
import {endpointLogin} from '../model/actions';
import { cookies } from '../model/reducers';

export default class OauthProcessComponent extends Component{

	constructor(props){
			
		super(props);
		const {tag} = this.props.match.params;
		this.processOAuth(tag);
	}

	processOAuth(tag){
		if(tag === "ExistingCredGoogleDrive"){
            setTimeout( () => {eventEmitter.emit(
               "errorOccured","Credential for the endpoint already Exists. Please logout from Google Drive and try again."
            )}, 500);
		}else if(tag === "ExistingCredDropbox"){
            setTimeout( () => { eventEmitter.emit(
                "errorOccured","Credential for the endpoint already Exists. Please logout from Dropbox and try again."
            )}, 500);
        }
		else if(tag === 'uuid'){
			console.log('User has opted to save auth tokens at ODS servers');
			console.log('UUID received');
			endpointLogin(DROPBOX_TYPE, sideLeft, {uuid: tag});
		}
		else{
			let qs = this.props.location.search;
			let qsObj = JSON.parse(decodeURIComponent(qs.substring(qs.indexOf('=') + 1)));

			if(tag === 'dropbox' ){
				console.log('Dropbox oAuth identifier received');
				this.updateLocalCredStore(DROPBOX_NAME, qsObj);
			}
			else if(tag === 'googledrive'){
				console.log('Google drive oAuth identifier received');
				this.updateLocalCredStore(GOOGLEDRIVE_NAME, qsObj);
			}
			else if(tag === 'gridftp'){
				console.log('GridFTP oAuth identifier received');
				this.updateLocalCredStore(GRIDFTP_NAME, qsObj);
			}
		}
	}

	updateLocalCredStore(protocolType, qsObj){
		let creds = cookies.get(protocolType) || 0;
		if(creds !== 0){
			let parsedJSON = JSON.parse(creds);
			parsedJSON.push({name : qsObj.name.split(":+")[1], token : qsObj.token });
			cookies.set(protocolType, JSON.stringify(parsedJSON));
		}
		else{
			cookies.set(protocolType, JSON.stringify([{name : qsObj.name.split(":+")[1], token : qsObj.token }]));
		}
	}

	render(){
		return <div>
			<Redirect  to={transferPageUrl}></Redirect>
			<h1> 
				Wait a second, You will be redirected.
			</h1>
		</div>
	}
}