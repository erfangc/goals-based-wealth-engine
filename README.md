# Wealth engine

Wealth Engine is a server that handles an advisor's workflow from connecting to accounts and automatically recommending
portfolios to meet clients' investment goals and automatically reminding people to re-balance

# High level proposal workflow

[![](https://mermaid.ink/img/eyJjb2RlIjoiZ3JhcGggVERcbiAgQVtTdGFydF0gLS0-IEJbR29hbHMgRW5naW5lXVxuICBCIC0tPnzOvCwgz4MsIE90aGVyIGNvbnN0cmFpbnRzLCBFeGlzdGluZyBwb3J0Zm9saW9zfCBDW0NvbnZleCBPcHRpbWl6ZXJdXG4gIEMgLS0-fFByb3Bvc2VkIHBvcnRmb2xpb3N8IERbQW5hbHlzaXMgRW5naW5lc11cbiAgRCAtLT58QW5hbHlzaXN8IEVbRW5kXVxuXG5cdCIsIm1lcm1haWQiOnsidGhlbWUiOiJkZWZhdWx0In19)](https://mermaid-js.github.io/mermaid-live-editor/#/edit/eyJjb2RlIjoiZ3JhcGggVERcbiAgQVtTdGFydF0gLS0-IEJbR29hbHMgRW5naW5lXVxuICBCIC0tPnzOvCwgz4MsIE90aGVyIGNvbnN0cmFpbnRzLCBFeGlzdGluZyBwb3J0Zm9saW9zfCBDW0NvbnZleCBPcHRpbWl6ZXJdXG4gIEMgLS0-fFByb3Bvc2VkIHBvcnRmb2xpb3N8IERbQW5hbHlzaXMgRW5naW5lc11cbiAgRCAtLT58QW5hbHlzaXN8IEVbRW5kXVxuXG5cdCIsIm1lcm1haWQiOnsidGhlbWUiOiJkZWZhdWx0In19)

# Goals engine

We implement goals based investing as a potential step in the full proposal generation / re-balancing etc.
journey
 
The input to the goals based process is a set of goals such as retirement income and major purchases and
an investment time horizon. The output from the goals based process is an ideal portfolio that maximizes the client's
chance of obtaining that goal described by it mean return and volatility (μ / σ)

We do not actually compute what this portfolio should be in terms of allocation until the next step (convex optimization)

The methodology implements this 2019 [paper](https://srdas.github.io/Papers/DP_Paper.pdf). Please see details on link

**Supported Paradigms**

 1. Model Portfolio Based
 
 In the model portfolio based paradigm, a set of model portfolios are used as input to the planning algorithm. The 
 model portfolio whose expected return and volatility maximizes the chance that your client's probability of achieving 
 their goals will be used
 
 2. Efficient Frontier Based
 
 The efficient frontier based paradigm accepts a list of assets from which an efficient frontier can be created. In this approach
 the target portfolio is build dynamically based on the point on this efficient frontier that corresponds to a portfolio
 that maximizes the probability of achieving the client's goal

Support for multiple goals must be available

**How do we model de-accmulation?**

Two step process:

Retirement income stream is discounted and aggregated as a single PV (from the point of view of the year of retirement). This
is the lump sum at date T = retirement date

This lump sum is the _goal_, investment horizon is T

**How do we model pensions / social security payments?**

Predict or input separately, then subtract these payments from required retirement income. They amount to a net reduction
to the retirement income requirement and will also lower the required lump sum

# Convex optimization

While the goals based wealth engine produces a target return and risk level, it is up to the convex optimization algorithm
to select and allocate actual assets into the portfolio

Thus forming a two step process for constructing new portfolios:

 - Use goals to determine optimal risk/reward
 
 - Use convex optimization to target the risk reward level (or rather target return and minimize risk)
 
 - The convex optimization step can also incorporate other constraints the client might have such as tax loss constraints
 or concentration constraints

**Support multiple portfolios**

The convex optimization process must support the ability to re-balance multiple portfolios that may or may not be able 
to transfer between each other. In addition, ideally we should consider different white list per account though this is aspirational

Why: 401K, IRA and Roth IRA assets cannot be withdrawn and transferred. However, they form an integral part of the overall
goals based investment process
 
# Other analysis

We produce scenarios, risk and asset allocation for all portfolios via the services in their respective packages

# Supporting services

To support the main workflow of creating proposals, we also have services that provide raw inputs and perform commonly used
intermediate computations. These are enumerated below

 - `covariance` package to provide covariance matrices
 - `expectedreturns` package to provide asset expected returns
 - `assets` package to provide asset indicative data such as price as well as time series information

# Appendix

## Time series data

Example query:

```
wget https://query1.finance.yahoo.com/v7/finance/download/VXF?period1=1176163200&period2=1583366400&interval=1d&events=history&crumb=m6l2bRMEQLD
```
