import React, { Component } from 'react';
import PropTypes from "prop-types";
import {openDropboxOAuth, openGoogleDriveOAuth, history, dropboxCredList, listFiles} from "../../APICalls/APICalls";
import {DROPBOX_TYPE, GOOGLEDRIVE_TYPE, FTP_TYPE, SFTP_TYPE, GRIDFTP_TYPE, HTTP_TYPE, SCP_TYPE} from "../../constants";

import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import Button from "@material-ui/core/Button";
import TextField from '@material-ui/core/TextField';

import Divider from '@material-ui/core/Divider';
import DataIcon from '@material-ui/icons/Laptop';
import BackIcon from '@material-ui/icons/KeyboardArrowLeft'
import AddIcon from '@material-ui/icons/AddToQueue';
import {getCred} from "./initialize_dnd.js";

import {eventEmitter} from "../../MainComponent";

const showText={
	dropbox: "DropBox",
	googledrive: "GoogleDrive",
	ftp : "FTP",
	sftp : "SFTP",
	http : "HTTP",
	gsiftp : "GridFTP",
	scp : "SCP"
}

const showType={
	dropbox: DROPBOX_TYPE,
	googledrive: GOOGLEDRIVE_TYPE,
	ftp : FTP_TYPE,
	sftp : SFTP_TYPE,
	http : HTTP_TYPE,
	gsiftp : GRIDFTP_TYPE,
	scp : SCP_TYPE
}
export default class EndpointAuthenticateComponent extends Component {
	static propTypes = {
		loginSuccess : PropTypes.func,
		endpoint : PropTypes.object,
		history: PropTypes.array,
		type: PropTypes.string,
		back: PropTypes.func,
		setLoading : PropTypes.func
	}
	constructor(props){
		super(props);
		this.state={
			history: props.history,
			endpoint: props.endpoint,
			credList: {},
			settingAuth: false,
			settingAuthType: "", 
			url: "",
			
			needPassword: false,
			username: "",
			password: "",
		};
		this.props.setLoading(true);

		dropboxCredList((data) =>{
			this.setState({credList: data});
			this.props.setLoading(false);
		}, (error) =>{
			this._handleError(error);
			this.props.setLoading(false);
		});
		this.handleChange = this.handleChange.bind(this);
		this._handleError = this._handleError.bind(this);
	}

	_handleError = (msg) =>{
    	eventEmitter.emit("errorOccured", msg);
	}

	handleChange = name => event => {
	    this.setState({
	      [name]: event.target.value,
	    });
	  };
	endpointCheckin=(url, credential, callback)=>{
		this.props.setLoading(true);
		listFiles(url, credential, (response)=>{
			const endpointSet = {
					uri: url,
					login: true,
					side: this.props.endpoint.side,
					credential: credential
				}
			this.props.setLoading(true);
			history(url, (suc)=>{
				console.log(suc)
			}, (error)=>{
				this._handleError(error);
				this.props.setLoading(false);
			})
			this.props.loginSuccess(endpointSet);
		}, (error)=>{
			this.props.setLoading(false);
			callback(error);
		})
	}
	render(){
		const {history, endpoint, credList, settingAuth, needPassword} = this.state;
		const { back, loginSuccess, setLoading} = this.props;
		const {uri} = endpoint;
		const histList = history.map((v) =>
			<ListItem button key={v} onClick={()=>{
				this.endpointCheckin(v, {}, (error)=>{
					this._handleError(error);
					this.setState({url: v, settingAuth: true, needPassword: true});
				})
			}}>
			  <ListItemIcon>
		        <DataIcon/>
		      </ListItemIcon>
	          <ListItemText primary={v} />
	        </ListItem>
		);
		
		const type = showText[endpoint.uri.split(":")[0]];
		const loginType = showType[endpoint.uri.split(":")[0]];
		console.log(type);
		const cloudList = Object.keys(credList).filter(id => {return (credList[id].name.toLowerCase().indexOf(type.toLowerCase()) != -1 && !getCred().includes(id))}).map((v) =>
			<ListItem button key={v} onClick={() => {
				const endpointSet = {
					uri: endpoint.uri,
					login: true,
					credential: {uuid: v},
					side: endpoint.side
				}
				//addCred(v, endpoint);
				loginSuccess(endpointSet);
			}}>
			  <ListItemIcon>
		        <DataIcon/>
		      </ListItemIcon>
	          <ListItemText primary={credList[v].name} />
	        </ListItem>
		);

		return(
		<div > 
			{!settingAuth && <List component="nav" style={{overflow: 'auto'}}>
		        <ListItem button onClick={() =>{
		        	back()
		        }}>
		          <ListItemIcon>
		          	<BackIcon/>
		          </ListItemIcon>
		          <ListItemText primary="Back" />
		        </ListItem>
		        <ListItem button onClick={() => {
		        	if(loginType == DROPBOX_TYPE){
		        		openDropboxOAuth();
		        	}else if(loginType == GOOGLEDRIVE_TYPE){
		        		openGoogleDriveOAuth();
		        	}else if(loginType == FTP_TYPE){
		        		this.setState({settingAuth: true, needPassword: false, url: "ftp://"});
		        	}else if(loginType == SFTP_TYPE){
		        		this.setState({settingAuth: true, needPassword: false, url: "sftp://"});
		        	}else if(loginType == GRIDFTP_TYPE){
		        		this.setState({settingAuth: true, needPassword: false, url: "gsiftp://"});
		        	}else if(loginType == HTTP_TYPE){
		        		this.setState({settingAuth: true, needPassword: false, url: "http://"});
		        	}else if(loginType == SCP_TYPE){
		        		this.setState({settingAuth: true, needPassword: false, url: "scp://"});
		        	}
		        }}>
		          <ListItemIcon>
		          	<AddIcon/>
		          </ListItemIcon>
		          <ListItemText primary={"Add New " + type} />
		        </ListItem>
		        <Divider />
		        {(loginType == DROPBOX_TYPE || loginType == GOOGLEDRIVE_TYPE) && cloudList}
		        {loginType != DROPBOX_TYPE && loginType != GOOGLEDRIVE_TYPE && histList}
		    </List>}

		    {settingAuth &&
		    	<div style={{flexGrow: 1, flexDirection: "column",}}>
		    	<Button style={{width: "100%", textAlign: "left"}} onClick={()=>{
		    		this.setState({settingAuth: false})}
		    	}><BackIcon/>Back</Button>
		    	<Divider />
		    	<TextField
		    	  style={{width: "80%"}}
		          id="outlined-name"
		          label="Url"
		          value={this.state.url}
		          onChange={this.handleChange('url')}
		          margin="normal"
		          variant="outlined"
		        />

		        
		        {needPassword &&
		        	<div>
			        <TextField
			    	  style={{width: "80%"}}
			          id="outlined-name"
			          label="Username"
			          value={this.state.username}
			          onChange={this.handleChange('username')}
			          margin="normal"
			          variant="outlined"
			        />
			        <TextField
			    	  style={{width: "80%"}}
			          id="outlined-name"
			          label="Password"
			          type="password"
			          value={this.state.password}
			          onChange={this.handleChange('password')}
			          margin="normal"
			          variant="outlined"
			        />
			        </div>
		    	}

		    	<Button style={{width: "100%", textAlign: "left"}} onClick={()=>{
		    		if(!needPassword){
			    		this.endpointCheckin(this.state.url, {}, ()=>{
			    			this.setState({needPassword: true});
			    		});
			    	}else{
			    		this.endpointCheckin(this.state.url, {type: "userinfo", username: this.state.username, password: this.state.password}, (msg)=>{
			    			this._handleError("Authentication Failed");
			    		});
			    	}
		    	}}>Next</Button>
		    	</div>
		    }
      	</div>);
	}
}