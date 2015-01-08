/** @jsx React.DOM */
var LikeButton = React.createClass({displayName: 'LikeButton',
  getInitialState: function() {
    return {liked: false};
  },
  handleClick: function(event) {
    this.setState({liked: !this.state.liked});
  },
  render: function() {
    var text = this.state.liked ? 'like' : 'unlike';
    var items = [1,2,3,4];
    return (
      React.DOM.ul({style: this.style}, 
         items.map(function(l){
            console.log('redraw');
            return React.DOM.li({key: l}, l, " ", React.DOM.a({href: "#", onClick: this.handleClick}, "link", l))
        }) 
      )
    );
  }
});

var Cmp = React.createClass({displayName: 'Cmp',
  render: function() {
    return React.DOM.p(null, 'text'); 
  }
}); 

React.renderComponent(
  LikeButton(null),
  document.getElementById('example')
);