package org.wordpress.android.stores.network.xmlrpc.site;

import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;

import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.action.SiteAction;
import org.wordpress.android.stores.model.SiteModel;
import org.wordpress.android.stores.model.SitesModel;
import org.wordpress.android.stores.network.UserAgent;
import org.wordpress.android.stores.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.stores.network.xmlrpc.BaseXMLRPCClient;
import org.wordpress.android.stores.network.xmlrpc.XMLRPCRequest;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.MapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SiteXMLRPCClient extends BaseXMLRPCClient {
    public SiteXMLRPCClient(Dispatcher dispatcher, RequestQueue requestQueue, AccessToken accessToken,
                            UserAgent userAgent) {
        super(dispatcher, requestQueue, accessToken, userAgent);
    }

    public void pullSites(final String xmlrpcUrl, final String username, final String password) {
        List<Object> params = new ArrayList<>(2);
        params.add(username);
        params.add(password);
        final XMLRPCRequest request = new XMLRPCRequest(
                xmlrpcUrl, "wp.getUsersBlogs", params,
                new Listener<Object>() {
                    @Override
                    public void onResponse(Object response) {
                        SitesModel sites = sitesResponseToSitesModel(response, username, password);
                        if (sites != null) {
                            mDispatcher.dispatch(SiteAction.UPDATE_SITES, sites);
                        } else {
                            // TODO: do nothing or dispatch error?
                        }
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.e(T.API, "Volley error", error);
                        // TODO: Error, dispatch network error
                    }
                }
        );
        add(request);
    }

    public void pullSite(final SiteModel site) {
        List<Object> params = new ArrayList<>(2);
        params.add(site.getSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        final XMLRPCRequest request = new XMLRPCRequest(
                site.getXMLRpcUrl(), "wp.getOptions", params,
                new Listener<Object>() {
                    @Override
                    public void onResponse(Object response) {
                        SiteModel updatedSite = updateSiteFromOptions(response, site);
                        mDispatcher.dispatch(SiteAction.UPDATE_SITE, updatedSite);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.e(T.API, "Volley error", error);
                    }
                }
        );
        add(request);
    }

    private SitesModel sitesResponseToSitesModel(Object response, String username, String password) {
        if (!(response instanceof Object[])) {
            return null;
        }
        Object[] responseArray = (Object[]) response;
        SitesModel sites = new SitesModel();
        for (Object siteObject: responseArray) {
            if (!(siteObject instanceof HashMap)) {
                continue;
            }
            HashMap<String, ?> siteMap = (HashMap<String, ?>) siteObject;
            SiteModel site = new SiteModel();
            // From the response
            site.setSiteId(Integer.parseInt((String) siteMap.get("blogid")));
            site.setName((String) siteMap.get("blogName"));
            site.setXMLRpcUrl((String) siteMap.get("xmlrpc"));
            site.setUrl((String) siteMap.get("xmlrpcEndpoint"));
            site.setIsAdmin((Boolean) siteMap.get("isAdmin"));
            // From what we know about the host
            site.setIsWPCom(false);
            site.setUsername(username);
            site.setPassword(password);
            sites.add(site);
        }

        if (sites.isEmpty()) {
            return null;
        }

        return sites;
    }

    private SiteModel updateSiteFromOptions(Object response, SiteModel oldModel) {
        Map<?, ?> blogOptions = (Map<?, ?>) response;
        oldModel.setName(getOption(blogOptions, "blog_title", String.class));
        oldModel.setSoftwareVersion(getOption(blogOptions, "software_version", String.class));
        oldModel.setIsFeaturedImageSupported(getOption(blogOptions, "post_thumbnail", Boolean.class));
        return oldModel;
    }

    private <T> T getOption(Map<?, ?> blogOptions, String key, Class<T> type) {
        Map<?, ?> map = (HashMap<?, ?>) blogOptions.get(key);
        if (map != null) {
            if (type == String.class) {
                return (T) MapUtils.getMapStr(map, "value");
            } else if (type == Boolean.class) {
                return (T) Boolean.valueOf(MapUtils.getMapBool(map, "value"));
            }
        }
        return null;
    }
}