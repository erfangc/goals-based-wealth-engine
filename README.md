# Wealth Engine

Wealth Engine is a server that handles an advisor's workflow from connecting to accounts and automatically recommending
portfolios to meet clients' investment goals and automatically reminding people to re-balance

# Goals based investing

We implement goals based investing as a potential step in the full proposal generation / re-balancing etc.
journey
 
The input to the goals based process is a set of goals such as retirement income and major purchases and
an investment time horizon. The output from the goals based process is an ideal portfolio that maximizes the client's
chance of obtaining that goal described by it mean return and volatility (mu / sigma)

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

# Convex optimization

While the goals based wealth engine produces a target return and risk level, it is up to the convex optimization algorithm
to select and allocate actual assets into the portfolio

Thus forming a two step process for constructing new portfolios:

 - Use goals to determine optimal risk/reward
 
 - Use convex optimization to target the risk reward level (or rather target return and minimize risk)
 
 - The convex optimization step can also incorporate other constraints the client might have such as tax loss constraints
 or concentration constraints
 
# Other analysis

We produce scenarios, risk and asset allocation for all portfolios via the services in their respective packages


