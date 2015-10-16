# Essentials

Four HTTP verbs are used to access resources:

**GET** - Use when requesting an existing resource

**POST** - Use when creating a new resource

**PUT** - Use when updating an existing resource

**DELETE** - Use when deleting a resource

## About Request and Response Formats

Requests and responses can be provided in either JSON or XML. Set the HTTP Accept header to specify the response format and the HTTP Content-Type header to specify the request format.

JSON

```
Accept: application/json
Content-Type: application/json
```

XML

```
Accept: application/xml
Content-Type: application/xml
```

## All about the tokens

From your description of the problem, it sounds like you're re-authorising your integration from scratch every day, rather than making use of OAuth's token system.
When you authorise your integration against your client's account, you're granted an **authorisation token**. This is then exchanged for an **access token** AND a **refresh token**.

As you've noticed, the access token is only useful for 24 hours. You can, however, use your **refresh token** to retrieve a **new access** token at any time. This can be done without the need to re-authenticate your application from scratch.

More information on how this works can be found in the OAuth section of our documentation:
https://dev.freeagent.com/docs/oauth

## Formatting XML

There are five characters which must be substituted by XML Entities when creating XML. These are:

Character | XML Entity
----------|--------
& | &amp;
< | &lt;
> | &gt;
" | &quot;
' | &apos;

# Freeagent Details

Create an app [here](https://dev.freeagent.com/apps). This will give you the OAuth identifier (client id) and OAuth secret (client secret).

[Source](https://dev.freeagent.com/docs/quick_start)

Account address: https:/bissonconsulting.sandbox.freeagent.com/
Email address	: diethard.steiner@bissolconsulting.com


To use the production API change the server in the examples below to:
api.freeagent.com

[Google OAuth 2.0 Playground](https://developers.google.com/oauthplayground)

To test Freeagent with Google OAuth 2.0 Playground, use [this URL](https://code.google.com/oauthplayground/#step3&url=https%3A//&content_type=application/json&http_method=GET&useDefaultOauthCred=unchecked&oauthEndpointSelect=Custom&oauthAuthEndpointValue=https%3A//api.sandbox.freeagent.com/v2/approve_app&oauthTokenEndpointValue=https%3A//api.sandbox.freeagent.com/v2/token_endpoint&includeCredentials=unchecked) which sets up most things! 

The URL above sets up the Playground with FreeAgent's OAuth Authorization Endpoint:

https://api.sandbox.freeagent.com/v2/approve_app
and the OAuth Token Endpoint:

https://api.sandbox.freeagent.com/v2/token_endpoint
Click on the Cog icon and enter the OAuth Client ID and OAuth Client Secret for the App created above.

With the Link icon you can create a link to save these settings for future use and if you tick Include OAuth credentials and OAuth tokens in the link you won't have to enter the Client ID and Secret each time.

Enter some text in the Scope box. Google requires this to have a value but the FreeAgent API doesn't enforce its use.

Click Authorize APIs.

The Playground will redirect you to the Sandbox where you will have to log in to the FreeAgent Sandbox account you created earlier. After log in you can approve the Playground app.

Approving the app will return you to the Playground. Click Exchange Authorization Code for Tokens to create access and refresh tokens which can be used to access the API.

You can then access the FreeAgent API for the Sandbox account you authorised. For example try:

```
https://api.sandbox.freeagent.com/v2/company
```

which should produce:

```
{"company":{"type":"UkLimitedCompany","currency":"GBP","mileage_units":"miles","company_start_date":"2010-07-01","sales_tax_registration_status":"Registered"}}
```

Now that you have an Access Token, you can also use it with Curl or use it to test out your own app with the FreeAgent API before you implement OAuth authentication.

My requests:

```
https://api.sandbox.freeagent.com/v2/contacts?view=active
https://api.sandbox.freeagent.com/v2/contacts?view=clients
https://api.sandbox.freeagent.com/v2/users
https://api.sandbox.freeagent.com/v2/expenses
https://api.sandbox.freeagent.com/v2/projects?view=active
https://api.sandbox.freeagent.com/v2/timeslips
```

Note: In prod remove `sandbox` in the URLs above!

Using CURL:

```
curl https://api.sandbox.freeagent.com/v2/company \
  -H "Authorization: Bearer TOKEN" \
  -H "Accept: application/xml" \
  -H "Content-Type: application/xml" \
  -X GET 
```

Replace `TOKEN` with your actual **Access Token**.

## Using CURL

[Source](https://dev.freeagent.com/docs/using_curl)

For simplicity get the access token from the Google OAuth 2.0 Playground. It can be found in *Step 2: Exchange authorization code tokens*.

> **Note**: The **Access Token** EXPIRES after a certain amount of time and need to be refreshed. There should be an auto-refresh option available.

[Source](http://outlandish.com/blog/freeagent-api-php-oauth2/)
  
curl https://api.sandbox.freeagent.com/v2/approve_app \
  -H "Location: http://localhost" \
  -H "Accept: application/json" \
  -H "Content-Type: application/json" \
  -X GET 


curl --user diethard.steiner@bissolconsulting.com:8qX-LDu-gwR-YTv https://api.sandbox.freeagent.com/v2/login \
  -H "Location: http://localhost" \
  -H "Accept: application/json" \
  -H "Content-Type: application/json" \
  -X GET 
  
  
http://members.orcid.org/api/methods-generate-access-token-testing

## Using httpbin

http://httpbin.org


----------

# Curl

## Getting the Autorization Code

[Source](http://members.orcid.org/api/methods-generate-access-token-testing)

Please proceed in the following sequence:

### Log in to a website

We want to log on to the Freeagent website with curl. We do not have to directly login via the official login page: We can just access any page by passing the username and password along with the `--user` parameter. We will also store the session details in a cookie so that we can later on reuse it (and hence don't have to login again):

```
curl -i -c cookies.txt -b cookies.txt --user diethard.steiner@bissolconsulting.com:8qX-LDu-gwR-YTv https://bissonconsulting.sandbox.freeagent.com/
```


Let's just check if the check if our cookie is working by accessing another page:

```
curl -i -c cookies.txt -b cookies.txt https://bissonconsulting.sandbox.freeagent.com/contacts
```

If successful, this will return the html content of the requested page.


### Request Authorization

Now that we simulated a client login and stored the session info in a cookie, we want to authorize our app.


```
curl -i -c cookies.txt -b cookies.txt  https://api.sandbox.freeagent.com/v2/approve_app?redirect_uri=https%3A%2F%2Fwww.bissolconsulting.com&response_type=code&client_id=oEj6KxiTYWldiHoQWhYvog&access_type=offline
```

Following parameters are required (see for more details [here](https://dev.freeagent.com/docs/oauth#client-libraries)):

- **client_id**: This is the FreeAgent app specific `OAuth identifier`. You can find it [here](https://dev.freeagent.com/apps).
- **response_type**: set to `code`
- **redirect_url**: set to any value

> **Note**: If you are unsure as to how to create the URL, pay attentioning to the output the Google OAuth 2.0 Playground generates with each step.

[Open]: So the problem now is that I am supposed to login/confirm my credentials, afer which the user gets redirected ... so how does this work on curl?

### Request Access Token


- Username and Password: OAuth userid and secret
- `code`: authorization code

```
curl -c cookies.txt -b cookies.txt --data "grant_type=authorization_code&code=0a40e606338407e5de73d2a82e2084caf50d0f9b&redirect_uri=https%3A%2F%2Fclient%2Eexample%2Ecom%2Fcb" --user oEj6KxiTYWldiHoQWhYvog:tsT1bvwWpmcJGjCHmFIhdQ https://api.freeagent.com/v2/token_endpoint
```