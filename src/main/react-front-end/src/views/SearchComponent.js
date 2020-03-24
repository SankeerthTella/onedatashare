import React, { Component } from 'react';
//import { makeStyles } from '@material-ui/core/styles';
import TextField from '@material-ui/core/TextField';
import Button from '@material-ui/core/Button';
import Box from '@material-ui/core/Box';
import { getSearchJobs } from '../APICalls/APICalls';

// const useStyles = makeStyles(theme => ({
//     root: {
//       '& .MuiTextField-root': {
//         marginRight: theme.spacing(1),
//         width: 200,
//       },
//     },
//     strech: {
//         marginRight: theme.spacing(3),
//         width: 200,
//     }
// }));

// const theme = {
//     spacing: value => value ** 2,
// }

export default class SearchComponent extends Component {
    constructor(props) {
        super(props);
        this.state = {
            username: '',
            jobid: '',
            progress: '',
            searchBtnEnable: 'false' //make it '' to enable
        };
        //const classes = useStyles();
    }

    componentDidMount() {
        //console.info('');
    }

    handleSearch() {
        //console.log('Search called');
        const { username, jobid, progress } = this.state
		getSearchJobs(
            username, 
            jobid, 
            progress,
            this.props.page,
            this.props.rowsPerPage,
            this.props.orderBy,
            this.props.order,
			this.props.refreshSuccess,
			this.props.refreshFailure
		)
    }

    handleUsernameChange(event) {
        //console.log('handleUsernameChange called', event);
        let searchBtn = ''; //enable
        if(event.target.value === '' && this.state.jobid === '' && this.state.progress === '')
            searchBtn = 'false';
        this.setState({
            username: '^hir',//event.target.value,
            searchBtnEnable: searchBtn
        });
    }

    handleJobIdChange(event) {
        //console.log('handleJobIdChange called');
        let searchBtn = ''; //enable
        if(event.target.value === '' && this.state.username === '' && this.state.progress === '')
            searchBtn = 'false';
        this.setState({
            jobid: event.target.value,
            searchBtnEnable: searchBtn
        });
    }

    handleProgressChange(event) {
        //console.log('handleProgressChange called');
        let searchBtn = ''; //enable
        if(event.target.value === '' && this.state.username === '' && this.state.jobid === '')
            searchBtn = 'false';
        this.setState({
            progress: event.target.value,
            searchBtnEnable: searchBtn
        });
    }
    
    render() {
           
        return ( 
            <Box>
                <TextField id="search-username" label="UserName(e.g.: xyz@email.com)"  onChange={(event) => this.handleUsernameChange(event)} style={{ marginRight: 15, width: 240 }}/>
                <TextField id="search-jobid" label="Job-ID(e.g.: 100-200)" onChange={(event) => this.handleJobIdChange(event)} style={{ marginRight: 15, width: 240 }}/>
                <TextField id="search-progress" label="Progress(e.g.: complete)" onChange={(event) => this.handleProgressChange(event)} style={{ marginRight: 15, width: 240 }}/>
                <Button variant="contained" size="large" color="primary" disabled={this.state.searchBtnEnable} onClick={() => this.handleSearch()} style={{ width: 150 }}>
                Search
                </Button>
            </Box>
        );
    };

};

//export default SearchComponent;