/** @jsx React.DOM */
var LikeButton = React.createClass({displayName: 'LikeButton',
  getInitialState: function() {
    return {items:[0,1,2,3]};
  },
  handleClick: function(event) {

  },
  addItem: function(){
      var temp = this.state.items.push(this.state.items.length);
      this.setState({items: this.state.items});
  },
  render: function() {
    var text = this.state.liked ? 'like' : 'unlike';
    var self = this;
    return (
      React.DOM.div({style: this.style}, 
        React.DOM.button({onClick: this.addItem}, "Add item"), ",", 
        React.DOM.p(null, "para"), ",", 
         this.state.items.map(function(l){
            console.log('redraw');
            return Cmp({key: l, val: "test", onClick: self.handleClick}, "link", l)
        }) 
      )
    );
  }
});

var Cmp = React.createClass({displayName: 'Cmp',
  render: function() {
    return React.DOM.p(null, " ", this.props.val, " ");
  }
});

React.renderComponent(
  LikeButton(null),
  document.getElementById('example')
);