/** @jsx React.DOM */
var LikeButton = React.createClass({displayName: 'LikeButton',
  getInitialState: function() {
    return {items:[1,2,3,4]};
  },
  handleClick: function(event) {
    var temp = this.state.items.push(this.state.items.length);
      console.log('click');
    //this.setState({liked: !this.state.liked});
  },
  render: function() {
    var text = this.state.liked ? 'like' : 'unlike';
    var self = this;
    return (
      React.DOM.div({style: this.style}, 
         this.state.items.map(function(l){
            console.log('redraw');
            return React.DOM.p({key: l, onClick: self.handleClick}, "link", l)
        }), 
        React.DOM.p({onClick: self.handleClick}, "other")
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