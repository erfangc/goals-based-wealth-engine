Asset Parser
--------

In prototype mode we receive asset cross section and time series data by parsing various internet sources
as well as buying widely available data

Here is a list of data sources

## To get a list of mutual funds:

The website [https://mutualfunds.com/](https://mutualfunds.com/) have a nice table and API to retrieve a list of 
funds (sorted by Net Asset Value among other options)

```
#
# the cURL command to issue against mutualfund.com for a list of the most
# popular mutual funds
#
curl 'https://mutualfunds.com/api/data_set/' \
-H 'user-agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.132 Safari/537.36' \
-H 'content-type: application/json' \
-H 'origin: https://mutualfunds.com' \
-H 'referer: https://mutualfunds.com/categories/all-funds/us-funds/' \
-H 'accept-language: en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7,zh-TW;q=0.6' \
--data-binary '{"tm":"1-fund-category","r":"Channel#531","only":["meta","data"],"page":1,"default_tab":"overview"}' \
--compressed
```

## Retrieving information on a particular mutual fund

We can use Yahoo Finance to retrieve data for specific funds once we obtained a whole list