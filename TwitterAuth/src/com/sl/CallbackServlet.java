/*
Copyright (c) 2007-2009, Yusuke Yamamoto
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the Yusuke Yamamoto nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY Yusuke Yamamoto ``AS IS'' AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL Yusuke Yamamoto BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.sl;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sl.dao.TwitterDAO;
import com.sl.db.DBException;
import com.sl.pojo.UserPojo;

import java.io.IOException;

@WebServlet("/callback")
public class CallbackServlet extends HttpServlet {
    private static final long serialVersionUID = 1657390011452788111L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Get twitter object from session
		Twitter twitter = (Twitter) request.getSession().getAttribute("twitter");
		//Get twitter request token object from session
		RequestToken requestToken = (RequestToken) request.getSession().getAttribute("requestToken");
		String verifier = request.getParameter("oauth_verifier");
		try {
			// Get twitter access token object by verifying request token 
		    AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verifier);
		    request.getSession().removeAttribute("requestToken");
		    
		    // Get user object from database with twitter user id
		    UserPojo user = TwitterDAO.selectTwitterUser(accessToken.getUserId());
		    if(user == null) {
		       // if user is null, create new user with given twitter details 
		       user = new UserPojo();
		       user.setTwitter_user_id(accessToken.getUserId());
		       user.setTwitter_screen_name(accessToken.getScreenName());
		       user.setAccess_token(accessToken.getToken());
		       user.setAccess_token_secret(accessToken.getTokenSecret());
		       TwitterDAO.insertRow(user);
		       user = TwitterDAO.selectTwitterUser(accessToken.getUserId());
		    } else {
		       // if user already there in database, update access token
		       user.setAccess_token(accessToken.getToken());
		       user.setAccess_token_secret(accessToken.getTokenSecret());
		       TwitterDAO.updateAccessToken(user);
		    }
		    request.setAttribute("user", user);
		} catch (TwitterException | DBException e) {
		    throw new ServletException(e);
		} 
		request.getRequestDispatcher("/status.jsp").forward(request, response);
    }
}
