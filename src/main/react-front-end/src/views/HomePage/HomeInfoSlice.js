import React, {Component} from "react";
import Hidden from "@material-ui/core/Hidden";
import Grid from "@material-ui/core/Grid";
import Transfer from "./mockups/transfer.png";

export default class HomeInfoSlice extends Component {

    constructor(props) {
        super(props);
    }

    makeWideView() {
        if (this.props.imgOnLeft) {
            return (
                <Grid container direction='row'>
                    <Grid container xs={3}>
                        <img className='homeImg' src={this.props.img} alt={this.props.imgAltTxt} />
                    </Grid>
                    <Grid container direction='column' xs={9}>
                        <h2>{this.props.title}</h2>
                        <p>{this.props.text}</p>
                    </Grid>
                </Grid>
            );
        }
        return (
            <Grid container direction='row'>
                <Grid container xs={9}>
                    <h2>{this.props.title}</h2>
                    <p>{this.props.text}</p>
                </Grid>
                <Grid container xs={3}>
                    <img className='homeImg' src={this.props.img} alt={this.props.altImgTxt}/>
                </Grid>
            </Grid>
        );

    }

    render() {
        let wideView = this.makeWideView();

        return (
            <div className='HomeInfoSlice'>
                {/* This renders desktop view */}
                    {wideView}
                {/* This renders mobile view */}
                <Hidden mdUp>
                    <Grid container direction='column'>
                        <Grid container xs={12}>
                            <img className='homeImg' width={'500px'} src={this.props.img} alt={this.props.altImgTxt} />
                        </Grid>
                        <Grid container xs={12}>
                            <h2>{this.props.title}</h2>
                            <p>{this.props.text}</p>
                        </Grid>
                    </Grid>
                </Hidden>
            </div>
        );
    }
};