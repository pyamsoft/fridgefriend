# FridgeFriend

FridgeFriend is an open source application which serves as a psuedo shopping
list manager, a sort-of fridge inventory management tool, and a kind-of
reminder tool all in one.

[![Get it on Google Play](https://raw.githubusercontent.com/pyamsoft/fridgefriend/main/art/google-play-badge.png)][1]

# What

Before going shopping, you can tell FridgeFriend which items you plan to buy,
including how many of each item - 3 boxes of strawberries, 2 bushels of kale,
5 apples - and it will remember your shopping list. Then, as you shop around
at the store you can check each of these items off your list and it will leave
your "needed items" shopping list and enter your "owned items" refrigerator
list.

When you attempt to add an item to your shopping list that you already have in
the fridge, FridgeFriend will let you know. If you choose to tell FridgeFriend
when your item will expire it will keep note of that too - and smartly remind
you when your items are getting close to, or have expired. And don't worry
about getting the date exact. Each time you get the same item, FridgeFriend
will keep track of the previous expiration dates and will be able to suggest
for you the correct expiration period based on all of your previous purchases.
It may even be able to tell you cases where you consistently consume an item
before its expiration date and remind you to purchase some extra.

FridgeFriend can also notify you when you have items on your shopping list and
are within close distance to one of your local grocery stores or super
markets. Since FridgeFriend respects your privacy, it will only inform you of
the stores that you ask it to keep track of. Once you get close to a store,
FridgeFriend can inform you about the items still on your shopping list - and
may be able to suggest to you the stores which you most frequently purchase
each item from.

All of this brainpower is local on your device, so there is no Internet
and no tracking or analytics of any kind. FridgeFriend is entirely
as smart as you let it be.

## Development

FridgeFriend is developed in the Open on GitHub at:  
```
https://github.com/pyamsoft/fridgefriend
```
If you know a few things about Android programming and are wanting to help
out with development you can do so by creating issue tickets to squash bugs,
and propose feature requests for future inclusion.`

# Issues or Questions

Please post any issues with the code in the Issues section on GitHub. Pull Requests
will be accepted on GitHub only after extensive reading and as long as the request
goes in line with the design of the application. Pull Requests will only be
accepted for new features of the application, for general purpose bug fixes, creating
an issue is simply faster.

[1]: https://play.google.com/store/apps/details?id=com.pyamsoft.fridge

## License

Apache 2

```
Copyright 2020 Peter Kenji Yamanaka

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

