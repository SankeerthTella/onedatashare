import React, {Component} from "react";
import Hidden from "@material-ui/core/Hidden";
import Grid from "@material-ui/core/Grid";

export default class HomeInfoSlice extends Component {

    makeWideView() {
        if (this.props.imgOnLeft) {
            return (
                <Grid container direction='row'>
                    <Grid container xs={5}>
                        <img className='homeImg homeImgLeft' src={this.props.img} alt={this.props.imgAltTxt} />
                    </Grid>
                    <Grid className='rightHomeTxt' container xs={7}>
                        <h2>{this.props.title}</h2>
                        <p>{this.props.text}</p>
                    </Grid>
                </Grid>
            );
        }
        return (
            <Grid container direction='row'>
                <Grid className='leftHomeTxt' container xs={7}>
                    <h2>{this.props.title}</h2>
                    <p>{this.props.text}</p>
                </Grid>
                <Grid container xs={5}>
                    <img className='homeImg homeImgRight' src={this.props.img} alt={this.props.altImgTxt}/>
                </Grid>
            </Grid>
        );

    }

    render() {
        let wideView = this.makeWideView();
        let name = this.props.className + ' HomeInfoSlice';
        return (
            <div className={name}>
                {/* This renders desktop view */}
                <Hidden smDown>
                    {wideView}
                </Hidden>
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