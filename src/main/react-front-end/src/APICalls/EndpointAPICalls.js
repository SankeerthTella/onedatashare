import { url, ENDPOINT_OP_URL, LIST_OP_URL, SHARE_OP_URL, MKDIR_OP_URL, SFTP_DOWNLOAD_URL, DEL_OP_URL, OAUTH_URL, BOX_TYPE, GRIDFTP_TYPE, GOOGLEDRIVE_TYPE, DROPBOX_TYPE } from '../constants';
import { axios, statusHandle } from "./APICalls";
import { getMapFromEndpoint, getIdsFromEndpoint } from '../views/Transfer/initialize_dnd.js';
import { cookies } from "../model/reducers";

function getUriType(uri) {
    return uri.split(":")[0].toLowerCase();
}

function buildEndpointOperationURL(baseURL, endpointType, operation) {
    return baseURL + "/" + endpointType + operation;
}

export async function listFiles(uri, endpoint, id, accept, fail) {
    let body = {
        uri: encodeURI(uri),
        id: id,
        portNumber: endpoint.portNumber,
    };

    body = Object.keys(endpoint.credential).length > 0 ? { ...body, credential: endpoint.credential } : body;

    let callback = accept;

    axios.post(buildEndpointOperationURL(ENDPOINT_OP_URL, getUriType(uri), LIST_OP_URL), JSON.stringify(body))
        .then((response) => {
            if (!(response.status === 200))
                callback = fail;
            statusHandle(response, callback);
        })
        .catch((error) => {
            statusHandle(error, fail);
        });
}

export async function share(uri, endpoint, accept, fail) {
    let callback = accept;

    axios.post(buildEndpointOperationURL(ENDPOINT_OP_URL, getUriType(uri), SHARE_OP_URL), {
        credential: endpoint.credential,
        uri: encodeURI(uri),
        map: getMapFromEndpoint(endpoint),

    })
        .then((response) => {
            if (!(response.status === 200))
                callback = fail;
            statusHandle(response, callback);
        })
        .catch((error) => {
            statusHandle(error, fail);
        });
}

export async function mkdir(uri, type, endpoint, accept, fail) {
    let callback = accept;

    const ids = getIdsFromEndpoint(endpoint);
    const id = ids[ids.length - 1];
    axios.post(buildEndpointOperationURL(ENDPOINT_OP_URL, getUriType(uri), MKDIR_OP_URL), {
        credential: endpoint.credential,
        uri: encodeURI(uri),
        id: id,
        map: getMapFromEndpoint(endpoint),
    })
        .then((response) => {
            if (!(response.status === 200))
                callback = fail;
            statusHandle(response, callback);
        })
        .catch((error) => {
            statusHandle(error, fail);
        });
}

export async function deleteCall(uri, endpoint, id, accept, fail) {
    let callback = accept;

    axios.post(buildEndpointOperationURL(ENDPOINT_OP_URL, getUriType(uri), DEL_OP_URL), {
        credential: endpoint.credential,
        uri: encodeURI(uri),
        id: id,
        map: getMapFromEndpoint(endpoint)
    })
        .then((response) => {
            if (!(response.status === 200))
                callback = fail;
            statusHandle(response, callback);
        })
        .catch((error) => {

            statusHandle(error, fail);
        });
}

// Returns the url for file. It is used to download the file and also to display in share url popup
async function getDownloadLink(uri, credential, _id) {
    return axios.post(buildEndpointOperationURL(ENDPOINT_OP_URL, getUriType(uri), OAUTH_URL), {
        credential: credential,
        uri: encodeURI(uri),
        id: _id,
    })
        .then((response) => {
            if (!(response.status === 200))
                console.log("Error in download API call");
            else {
                return response.data
            }
        })
        .catch((error) => {
            console.log("Error encountered while generating download link");
        });
}

export async function getSharableLink(uri, credential, _id) {
    return getDownloadLink(uri, credential, _id).then((response) => {
        return response
    })
}

export async function download(uri, credential, _id) {
    return getDownloadLink(uri, credential, _id).then((response) => {
        if (response !== "") {
            window.open(response)
        }
        else {
            console.log("Error encountered while generating download link");
        }
    })
}

export async function getDownload(uri, credential) {
    let json_to_send = {
        credential: credential,
        uri: uri,
    }

    const jsonStr = JSON.stringify(json_to_send);
    cookies.set("CX", jsonStr, { expires: 1 });

    window.location = buildEndpointOperationURL(ENDPOINT_OP_URL, getUriType(uri), SFTP_DOWNLOAD_URL);
    setTimeout(() => {
        cookies.remove("CX");
    }, 5000);
}


export async function openDropboxOAuth() {
    openOAuth(DROPBOX_TYPE);
}

export async function openGoogleDriveOAuth() {
    openOAuth(GOOGLEDRIVE_TYPE);
}

export async function openGridFtpOAuth() {
    openOAuth(GRIDFTP_TYPE);
}

export async function openBoxOAuth() {
    openOAuth(BOX_TYPE);
}

async function openOAuth(type) {
    window.location = buildEndpointOperationURL(ENDPOINT_OP_URL, getUriType(type), OAUTH_URL);
    // axios.get(buildEndpointOperationURL(ENDPOINT_OP_URL, getUriType(type), OAUTH_URL))
    //     .then(resp => {
    //         console.log(resp);
    //         window.location = resp.data.uri;
    //     }).catch(err => {
    //         console.log(err);
    //     });
}


export async function globusEndpointIds(gep, accept, fail) {
    let callback = accept;
    axios.post(url + 'globus', {
        action: 'endpointId',

        globusEndpoint: gep,
    }).then((response) => {
        if (!(response.status === 200))
            callback = fail;
        statusHandle(response, callback);
    })
        .catch((error) => {
            statusHandle(error, fail);
        });
}

export async function globusEndpointDetail(gep, accept, fail) {
    let callback = accept;
    axios.post(url + 'globus', {
        action: 'endpoint',
        globusEndpoint: gep,
    }).then((response) => {
        if (!(response.status === 200))
            callback = fail;
        statusHandle(response, callback);
    })
        .catch((error) => {
            statusHandle(error, fail);
        });
}

export async function globusEndpointActivate(gep, _username, _password, accept, fail) {
    let callback = accept;
    axios.post(url + 'globus', {
        action: 'endpointActivate',
        globusEndpoint: gep,
        username: _username,
        password: _password
    }).then((response) => {
        if (!(response.status === 200))
            callback = fail;
        statusHandle(response, callback);
    })
        .catch((error) => {
            statusHandle(error, fail);
        });
}

export async function deleteEndpointId(ged, accept, fail) {
    let callback = accept;

    axios.post(url + 'globus', {
        action: "deleteEndpointId",
        globusEndpoint: ged,
    })
        .then((response) => {
            if (!(response.status === 200))
                callback = fail;
            statusHandle(response, callback);
        })
        .catch((error) => {
            statusHandle(error, fail);
        });
}

export async function globusListEndpoints(filter_fulltext, accept, fail) {
    let callback = accept;
    return axios.post(url + 'globus', {
        action: "endpoint_list",
        filter_fulltext: filter_fulltext
    })
        .then((response) => {
            if (!(response.status === 200))
                callback = fail;
            statusHandle(response, callback);
        })
        .catch((error) => {

            statusHandle(error, fail);
        });
}
